package com.everis.bankms.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "BANK")
@EqualsAndHashCode(callSuper = false)
public class Bank {

  @Id
  private String id;
  @NotBlank(message = "'numId' is required")
  private String numId;
  @NotBlank(message = "'name' is required")
  private String name;
  private Set<String> clientProfiles;
  private HashMap<String, Integer> productMaxTrans;
  private double depRetComission;
  private double transactionComission;
  private double creditPayComission;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private Date joinDate;
}