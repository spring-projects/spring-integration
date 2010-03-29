package com.springframework.integration.loanbroker.loanshark.domain;

import java.lang.String;

privileged aspect LoanShark_Roo_ToString {
    
    public String LoanShark.toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Counter: ").append(getCounter()).append(", ");
        sb.append("AverageRate: ").append(getAverageRate());
        return sb.toString();
    }
    
}
