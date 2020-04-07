package com.everis.bankms.dto;


import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfilesDto {

  private Set<String> clientProfiles;
}