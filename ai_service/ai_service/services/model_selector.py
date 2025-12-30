from __future__ import annotations

"""Model selection & fallback utility for ai_service.

This centralises all LLM access so we can dynamically choose between
multiple providers (ChatGLM, Yi-6B, DeepSeek, qwen-plus) without touching calling
code.

Priority order is determined by evaluation results:
    1. glm-4  – best overall quality / latency
    2. qwen-plus – high reasoning capability
    3. yi-34b-chat    – best source-recall, longer context
    4. DeepSeek-V3   – rich code explanations, existing default

If the primary model is unavailable or raises an exception, the
selector will transparently fall back to the next one.

Environment variables expected (all optional – missing creds will skip
that backend):
    # ChatGLM3
    CHATGLM_API_KEY
    CHATGLM_BASE_URL          (default https://open.bigmodel.cn/api/paas/v4)
    CHATGLM_MODEL_NAME        (default glm-4)

    # qwen-plus
    QWEN_API_KEY
    QWEN_BASE_URL             (default https://dashscope.aliyuncs.com/compatible-mode/v1)
    QWEN_MODEL_NAME           (default qwen-plus)

    # yi-34b-chat (01.ai)
    YI_API_KEY
    YI_BASE_URL               (default https://openai.01.ai/v1)
    YI_MODEL_NAME             (default yi-34b-chat)

    # DeepSeek-V3 (existing)
    DEEPSEEK_API_KEY
    DEEPSEEK_BASE_URL         (default https://api.deepseek.com/v1)
    DEEPSEEK_MODEL_NAME       (default deepseek-chat)

Optionally override default priority by setting PRIMARY_LLM_MODEL to
"chatglm" | "qwen" | "yi" | "deepseek".
"""

import os
import logging
from typing import Dict, List, Optional

from openai import OpenAI

logger = logging.getLogger("model_selector")


class ModelSelector:
    """Simple round-robin / fallback model chooser for chat completion."""

    def __init__(self) -> None:
        self._clients: Dict[str, dict] = {}
        self._init_clients()

        # Default max tokens to avoid provider-side truncation (can be overridden per call)
        try:
            self.default_max_tokens: int = int(os.getenv("LLM_MAX_TOKENS", "4096"))
        except Exception:
            self.default_max_tokens = 4090

        # Determine default priority order (skip models without creds)
        default_order = [
            os.getenv("PRIMARY_LLM_MODEL", "chatglm"),  # user-preferred first
            "chatglm",
            "qwen",
            "yi",
            "deepseek",
        ]
        # Remove duplicates while preserving order and keeping only available
        seen = set()
        self.priority: List[str] = []
        for key in default_order:
            if key in self._clients and key not in seen:
                self.priority.append(key)
                seen.add(key)

        if not self.priority:
            raise RuntimeError("No valid LLM credentials found – cannot initialise ModelSelector")

        logger.info(f"ModelSelector initialised with priority {self.priority}")

    # ------------------------------------------------------------------
    # public helpers
    # ------------------------------------------------------------------
    def chat_completion(self, *, messages: List[dict], temperature: float = 0.7, top_p: float = 0.95, purpose: Optional[str] = None, **extra) -> any:  # noqa: ANN401
        """Try providers in a computed order (per-purpose override supported) until one succeeds.

        Priority resolution:
        1) If purpose provided and env LLM_PRIORITY_<PURPOSE> exists, use that order.
        2) Otherwise fall back to default priority derived at init.
        """

        order = self._get_priority_for_purpose(purpose)
        logger.info(
            "LLM call purpose=%s, provider order=%s",
            purpose or "<none>",
            order,
        )

        for key in order:
            meta = self._clients[key]
            client: OpenAI = meta["client"]
            model_name: str = meta["model_name"]

            try:
                # Build arguments with sane defaults to avoid truncation
                req_kwargs = {
                    "model": model_name,
                    "messages": messages,
                    "temperature": temperature,
                    "top_p": top_p,
                }
                # Allow callers to override; ensure max_tokens is always present unless explicitly set
                if "max_tokens" not in extra:
                    extra["max_tokens"] = self.default_max_tokens
                req_kwargs.update(extra)

                logger.debug("Attempting backend=%s model=%s with max_tokens=%s", key, model_name, req_kwargs.get("max_tokens"))
                response = client.chat.completions.create(**req_kwargs)
                # Success – log & return
                logger.info(
                    "LLM success via %s (model=%s, tokens=%s)",
                    key,
                    model_name,
                    getattr(response, "usage", "?"),
                )
                return response
            except Exception as exc:  # noqa: BLE001
                logger.warning("LLM backend %s failed: %s – trying fallback", key, exc)
                continue

        # If we reach here, all backends failed
        raise RuntimeError("All LLM backends failed to generate a response")

    # ------------------------------------------------------------------
    # internal helpers
    # ------------------------------------------------------------------
    def _init_clients(self) -> None:
        """Create OpenAI-compatible clients for each provider with creds."""
        # ChatGLM ---------------------------------------------------------
        chatglm_key = os.getenv("CHATGLM_API_KEY")
        if chatglm_key:
            base_url = os.getenv("CHATGLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4")
            model_name = os.getenv("CHATGLM_MODEL_NAME", "glm-4")
            client = OpenAI(api_key=chatglm_key, base_url=base_url)
            self._clients["chatglm"] = {
                "client": client,
                "model_name": model_name,
            }
            logger.info("ChatGLM backend enabled (%s)", model_name)
        else:
            logger.info("ChatGLM backend disabled – no CHATGLM_API_KEY found")
            
        # qwen-pluss --------------------------------------------------------
        qwen_key = os.getenv("QWEN_API_KEY")
        if qwen_key:
            base_url = os.getenv("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
            model_name = os.getenv("QWEN_MODEL_NAME", "qwen-pluss")
            client = OpenAI(api_key=qwen_key, base_url=base_url)
            self._clients["qwen"] = {
                "client": client,
                "model_name": model_name,
            }
            logger.info("qwen-plus backend enabled (%s)", model_name)
        else:
            logger.info("qwen-plus backend disabled – no QWEN_API_KEY found")

        # Yi-6B -----------------------------------------------------------
        yi_key = os.getenv("YI_API_KEY")
        if yi_key:
            base_url = os.getenv("YI_BASE_URL", "https://openai.01.ai/v1")
            model_name = os.getenv("YI_MODEL_NAME", "yi-34b-chat")
            client = OpenAI(api_key=yi_key, base_url=base_url)
            self._clients["yi"] = {
                "client": client,
                "model_name": model_name,
            }
            logger.info("Yi backend enabled (%s)", model_name)
        else:
            logger.info("Yi backend disabled – no YI_API_KEY found")

        # DeepSeek --------------------------------------------------------
        deepseek_key = os.getenv("DEEPSEEK_API_KEY")
        if deepseek_key:
            base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
            model_name = os.getenv("DEEPSEEK_MODEL_NAME", "deepseek-chat")
            client = OpenAI(api_key=deepseek_key, base_url=base_url)
            self._clients["deepseek"] = {
                "client": client,
                "model_name": model_name,
            }
            logger.info("DeepSeek backend enabled (%s)", model_name)
        else:
            logger.info("DeepSeek backend disabled – no DEEPSEEK_API_KEY found") 

    # ------------------------------------------------------------------
    # Per-purpose priority helpers
    # ------------------------------------------------------------------
    def _normalize_backend_id(self, ident: str) -> Optional[str]:
        ident = (ident or "").strip().lower()
        if ident in {"glm", "chatglm"}:
            ident = "chatglm"
        if ident in {"qwen", "qwen-plus", "qwenmax", "qwen_max"}:
            ident = "qwen"
        if ident in {"yi", "01ai", "yi-34b", "yi-34b-chat"}:
            ident = "yi"
        if ident in {"deepseek", "deepseek-v3", "deepseek_chat", "deepseek-chat"}:
            ident = "deepseek"
        return ident if ident in self._clients else None

    def _parse_priority_env(self, raw: str) -> list[str]:
        # Accept separators: ',', '>', '->', whitespace
        if not raw:
            return []
        tokens: list[str] = []
        # Replace arrows with commas, then split
        cleaned = raw.replace("->", ",").replace(">", ",")
        for part in cleaned.split(","):
            norm = self._normalize_backend_id(part)
            if norm and norm not in tokens:
                tokens.append(norm)
        # Keep only available backends
        return [t for t in tokens if t in self._clients]

    def _purpose_env_key(self, purpose: Optional[str]) -> str:
        if not purpose:
            return ""
        # Uppercase and replace non-alnum with underscore
        import re as _re  # local alias to avoid top-level import noise
        key = _re.sub(r"[^A-Za-z0-9]+", "_", purpose).strip("_").upper()
        return f"LLM_PRIORITY_{key}"

    def _get_priority_for_purpose(self, purpose: Optional[str]) -> list[str]:
        # 1) Per-purpose override from env
        env_key = self._purpose_env_key(purpose)
        if env_key:
            raw = os.getenv(env_key, "").strip()
            if raw:
                order = self._parse_priority_env(raw)
                if order:
                    # Log chosen env override
                    logger.info("Using per-purpose priority from %s = %s", env_key, order)
                    return order

        # 2) Optional global default override
        raw_default = os.getenv("LLM_PRIORITY_DEFAULT", "").strip()
        if raw_default:
            order = self._parse_priority_env(raw_default)
            if order:
                logger.info("Using global priority from LLM_PRIORITY_DEFAULT = %s", order)
                return order

        # 3) Fallback to initialised default order (respects PRIMARY_LLM_MODEL)
        return list(self.priority)