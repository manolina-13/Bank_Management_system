package model;

// CREATE TABLE Request (

//   id INTEGER PRIMARY KEY AUTOINCREMENT,
//   type TEXT,
//   data_json TEXT,
//   status TEXT,
//   remarks TEXT,
//   related_to_acc_no TEXT,
//   created_by_staff_no TEXT,
//   approved_by_manager_no TEXT
// );

public class Request {
    public int id;
    public String type;
    public String dataJson;
    public String status;
    public String remarks;
    public String relatedToAccNo;
    public String createdByStaffNo;
    public String approvedByManagerNo;
    public String createdAt;

    public Request(int id, String type, String dataJson, String status, String remarks, String relatedToAccNo,
            String createdByStaffNo, String approvedByManagerNo) {
        this.id = id;
        this.type = type;
        this.dataJson = dataJson;
        this.status = status;
        this.remarks = remarks;
        this.relatedToAccNo = relatedToAccNo;
        this.createdByStaffNo = createdByStaffNo;
        this.approvedByManagerNo = approvedByManagerNo;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDataJson() {
        return dataJson;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getRelatedToAccNo() {
        return relatedToAccNo;
    }

    public String getCreatedByStaffNo() {
        return createdByStaffNo;
    }

    public String getApprovedByManagerNo() {
        return approvedByManagerNo;
    }

    public String getCreatedAt() {
        return createdAt;

    }
}