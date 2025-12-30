"""
快速测试多模态聊天功能
"""
import requests
import json

# 配置
API_BASE = "http://localhost:8000"

def test_get_categories():
    """测试获取类别列表"""
    print("=== 测试获取心灵港湾类别 ===")
    response = requests.get(f"{API_BASE}/api/multimodal/categories?chat_type=harbor")
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
    print()
    
    print("=== 测试获取艺术天地类别 ===")
    response = requests.get(f"{API_BASE}/api/multimodal/categories?chat_type=art")
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
    print()

def test_harbor_chat():
    """测试心灵港湾对话"""
    print("=== 测试心灵港湾对话 ===")
    
    data = {
        'question': '我最近学习压力很大，总是担心考不好，怎么办？',
        'chat_type': 'harbor',
        'category': '学习压力',
        'temperature': '0.7',
        'max_tokens': '500'
    }
    
    response = requests.post(f"{API_BASE}/api/multimodal/chat", data=data)
    print(f"状态码: {response.status_code}")
    result = response.json()
    print(f"成功: {result.get('success')}")
    print(f"回答: {result.get('answer', '')[:200]}...")
    print(f"模型: {result.get('model')}")
    print()

def test_art_chat():
    """测试艺术天地对话"""
    print("=== 测试艺术天地对话 ===")
    
    data = {
        'question': '能介绍一下印象派的特点吗？',
        'chat_type': 'art',
        'category': '流派介绍',
        'temperature': '0.7',
        'max_tokens': '500'
    }
    
    response = requests.post(f"{API_BASE}/api/multimodal/chat", data=data)
    print(f"状态码: {response.status_code}")
    result = response.json()
    print(f"成功: {result.get('success')}")
    print(f"回答: {result.get('answer', '')[:200]}...")
    print(f"模型: {result.get('model')}")
    print()

def test_multimodal_chat_with_image():
    """测试多模态对话（带图片）"""
    print("=== 测试多模态对话（带图片）===")
    print("注意：需要提供图片文件路径")
    # 此测试需要实际图片文件，这里仅作示例
    # image_path = "test_image.jpg"
    # if os.path.exists(image_path):
    #     with open(image_path, 'rb') as f:
    #         files = {'files': f}
    #         data = {
    #             'question': '请帮我分析这幅画的构图和色彩',
    #             'chat_type': 'art',
    #             'category': '画作鉴赏'
    #         }
    #         response = requests.post(f"{API_BASE}/api/multimodal/chat", data=data, files=files)
    #         print(response.json())
    print("（需要图片文件，跳过）")
    print()

if __name__ == "__main__":
    print("多模态聊天功能测试")
    print("=" * 50)
    print()
    
    try:
        # 测试类别获取
        test_get_categories()
        
        # 测试心灵港湾
        test_harbor_chat()
        
        # 测试艺术天地
        test_art_chat()
        
        # 测试多模态
        test_multimodal_chat_with_image()
        
        print("=" * 50)
        print("测试完成！")
        
    except requests.exceptions.ConnectionError:
        print("❌ 连接失败！请确保 AI 服务已启动在 http://localhost:8000")
    except Exception as e:
        print(f"❌ 测试出错: {e}")
