package com.scholar.platform.service;

import com.scholar.platform.entity.PaperKeyword;
import com.scholar.platform.repository.PaperKeywordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;

@Service
public class PaperKeywordService {
    @Autowired
    private PaperKeywordRepository paperKeywordRepository;

    @Transactional
    public void updateKeywords(List<String> keywords) {
        int month = LocalDate.now().getMonthValue();
        for (String keyword : keywords) {
            Optional<PaperKeyword> optional = paperKeywordRepository.findByKeyword(keyword);
            PaperKeyword pk = optional.orElseGet(() -> {
                PaperKeyword newPk = new PaperKeyword();
                newPk.setKeyword(keyword);
                newPk.setCnt(0);
                setCntX(newPk, month, 0);
                return newPk;
            });
            pk.setCnt(pk.getCnt() == null ? 1 : pk.getCnt() + 1);
            int oldMonthCnt = getCntX(pk, month);
            setCntX(pk, month, oldMonthCnt + 1);
            paperKeywordRepository.save(pk);
        }
    }

    private void setCntX(PaperKeyword pk, int month, int value) {
        try {
            Field field = PaperKeyword.class.getDeclaredField("cnt" + month);
            field.setAccessible(true);
            field.set(pk, value);
        } catch (Exception ignored) {}
    }

    private int getCntX(PaperKeyword pk, int month) {
        try {
            Field field = PaperKeyword.class.getDeclaredField("cnt" + month);
            field.setAccessible(true);
            Object val = field.get(pk);
            return val == null ? 0 : (int) val;
        } catch (Exception e) {
            return 0;
        }
    }
}
