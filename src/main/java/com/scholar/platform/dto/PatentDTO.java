package com.scholar.platform.dto;

import com.scholar.platform.entity.Patent;
import lombok.Data;

@Data
public class PatentDTO {
    private String id;
    private String patentName;
    private String patentType;
    private String applicant;
    private String applicantType;
    private String applicationNumber;
    private Integer applicationYear;
    private String grantNumber;
    private Integer grantYear;
    private String ipcCode;
    private String inventor;
    private String abstractText;
    private Integer citedCount;

    public static PatentDTO fromEntity(Patent patent) {
        if (patent == null) return null;
        PatentDTO dto = new PatentDTO();
        dto.setId(patent.getId());
        dto.setPatentName(patent.getPatentName());
        dto.setPatentType(patent.getPatentType());
        dto.setApplicant(patent.getApplicant());
        dto.setApplicantType(patent.getApplicantType());
        dto.setApplicationNumber(patent.getApplicationNumber());
        dto.setApplicationYear(patent.getApplicationYear());
        dto.setGrantNumber(patent.getGrantNumber());
        dto.setGrantYear(patent.getGrantYear());
        dto.setIpcCode(patent.getIpcCode());
        dto.setInventor(patent.getInventor());
        dto.setAbstractText(patent.getAbstractText());
        dto.setCitedCount(patent.getCitedCount());
        return dto;
    }
}
