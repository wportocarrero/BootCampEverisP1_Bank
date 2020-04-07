package com.everis.bankms.service.impl;


import com.everis.bankms.dao.BankRepository;
import com.everis.bankms.dto.BankMaxTransDto;
import com.everis.bankms.dto.ClientProfilesDto;
import com.everis.bankms.dto.MessageDto;
import com.everis.bankms.model.Bank;
import com.everis.bankms.service.BankService;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BankServiceImpl implements BankService {

  private static final Logger logger = LoggerFactory.getLogger(BankServiceImpl.class);

  @Autowired
  private BankRepository bankRepo;


  @Override
  public Mono<Bank> findByName(String name) {
    return bankRepo.findByName(name);
  }

  @Override
  public Flux<Bank> findAll() {
    return bankRepo.findAll();
  }

  @Override
  public Mono<Bank> findById(String id) {
    return bankRepo.findByNumId(id);
  }


  @Override
  public Mono<Bank> save(Bank cl) {
    try {
      if (cl.getJoinDate() == null) {
        cl.setJoinDate(new Date());
      } else {
        cl.setJoinDate(cl.getJoinDate());
      }

      return bankRepo.save(cl);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @Override
  public Mono<Bank> update(Bank cl, String id) {
    try {
      return bankRepo.findById(id)
          .flatMap(dbClient -> {

            //JoinDate
            if (cl.getJoinDate() != null) {
              dbClient.setJoinDate(cl.getJoinDate());
            }

            //name
            if (cl.getName() != null) {
              dbClient.setName(cl.getName());
            }

            //name
            if (cl.getNumId() != null) {
              dbClient.setNumId(cl.getNumId());
            }

            //client profiles
            if (cl.getClientProfiles() != null) {
              //verificar que lista interna no sea nula
              if (dbClient.getClientProfiles() == null) {
                dbClient.setClientProfiles(new HashSet<>());
              }
              //combinar lista con lista interna y borrar duplicados
              Set<String> holders = dbClient.getClientProfiles();
              holders.addAll(cl.getClientProfiles());
            }

            return bankRepo.save(dbClient);

          }).switchIfEmpty(
              Mono.error(new Exception("No se pudo encontrar el banco que se quiere actualizar")));
    } catch (Exception e) {
      return Mono.error(e);
    }

  }

  @Override
  public Mono<Void> delete(String id) {
    try {
      return bankRepo.findById(id).flatMap(cl -> {
        return bankRepo.delete(cl);
      });
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @Override
  public Mono<Boolean> existsByNumId(String numId) {
    return bankRepo.existsByNumId(numId);
  }

  @Override
  public Mono<String> getNumId(String numId) {
    try {
      Mono<Bank> bank = bankRepo.findByNumId(numId).switchIfEmpty(Mono.justOrEmpty(new Bank()));
      Mono<String> ret = bank.map(cl -> {
        logger.info("ENCONTRE ESTO :" + cl.getName());
        if (cl.getNumId() != null) {
          return cl.getNumId();
        } else {
          return "-1";
        }
      });
      return ret;
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @Override
  public Mono<ClientProfilesDto> getClientProfiles(String numId) {
    try {
      return bankRepo.findByNumId(numId).map(bank -> {
        return new ClientProfilesDto(bank.getClientProfiles());
      }).switchIfEmpty(Mono.error(new Exception("Banco no encontrado")));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @Override
  public Mono<BankMaxTransDto> getbankComission(String numId) {
    try {
      return bankRepo.findByNumId(numId).map(bank -> {
        return new BankMaxTransDto(bank.getProductMaxTrans());
      }).switchIfEmpty(Mono.error(new Exception("Banco no encontrado")));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private Mono<MessageDto> depositRet(String numAccount, double money) {
    String url = "http://localhost:8010/bankprod/transaction/" + numAccount;
    return WebClient.create()
        .post()
        .uri(url)
        .bodyValue(money)
        .retrieve()
        .bodyToMono(MessageDto.class);
  }


  private Mono<MessageDto> payCreditDebt(String numAccount, String creditNumber) {
    String url = "http://localhost:8010/bankprod/payCreditDebt/" + numAccount + "/" + creditNumber;
    return WebClient.create()
        .post()
        .uri(url)
        .retrieve()
        .bodyToMono(MessageDto.class);
  }

  private Mono<MessageDto> bankTransaction(String numAccountOrigin, String numAccountDestination,
      double money) {
    String url = "http://localhost:8010/bankprod/bankProductTransaction/" + numAccountOrigin + "/"
        + numAccountDestination;
    return WebClient.create()
        .post()
        .uri(url)
        .bodyValue(money)
        .retrieve()
        .bodyToMono(MessageDto.class);
  }

  private Mono<String> getProductBankId(String numAccount) {
    String url = "http://localhost:8010/bankprod/getBankId/" + numAccount;
    return WebClient.create()
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class);
  }

  private Mono<MessageDto> chargeComission(String numAccount, double comission) {
    String url = "http://localhost:8010/bankprod/chargeExtComission/" + numAccount;
    return WebClient.create()
        .post()
        .uri(url)
        .bodyValue(comission)
        .retrieve()
        .bodyToMono(MessageDto.class);
  }

  @Override
  public Mono<MessageDto> otherBankDepositRet(String idBankOrigin, String numAccount,
      double money) {
    //que hago si me devuelve mono error??
    //verificar primero que banco existe
    return bankRepo.findByNumId(idBankOrigin).flatMap(bank -> {
      return getProductBankId(numAccount).flatMap(prodBankId -> {
        if (!prodBankId.equals(idBankOrigin)) {
          return depositRet(numAccount, money).flatMap(msResponse -> {
            if (msResponse.getCode().equals("1")) {
              //cobrar comision
              //devolver mensaje del servicio de cobro de comision
              return chargeComission(numAccount, bank.getDepRetComission());
            } else {
              return Mono.error(new Exception("Error en la transaccion"));
            }
          });
        } else {
          return Mono.error(new Exception("el producto pertenece a este banco"));
        }
      });
    }).switchIfEmpty(Mono.error(new Exception("banco no encontrado")));
  }


  @Override
  public Mono<MessageDto> otherBankTransaction(String idBankOrigin, String numAccountOri,
      String numAccountDes, double money) {
    //que hago si me devuelve mono error??
    //verificar primero que banco existe
    return bankRepo.findByNumId(idBankOrigin).flatMap(bank -> {
      return getProductBankId(numAccountOri).flatMap(prodBankId -> {
        if (!prodBankId.equals(idBankOrigin)) {
          return bankTransaction(numAccountOri, numAccountDes, money).flatMap(msResponse -> {
            if (msResponse.getCode().equals("1")) {
              //cobrar comision
              //devolver mensaje del servicio de cobro de comision
              return chargeComission(numAccountOri, bank.getTransactionComission());
            } else {
              return Mono.error(new Exception("Error en la transaccion"));
            }
          });
        } else {
          return Mono.error(new Exception("el producto pertenece a este banco"));
        }
      });
    }).switchIfEmpty(Mono.error(new Exception("banco no encontrado")));
  }


  @Override
  public Mono<MessageDto> otherBankPayCreditDebt(String idBankOrigin, String numAccount,
      String creditNumber) {
    return bankRepo.findByNumId(idBankOrigin).flatMap(bank -> {
      return getProductBankId(numAccount).flatMap(prodBankId -> {
        if (!prodBankId.equals(idBankOrigin)) {
          return payCreditDebt(numAccount, creditNumber).flatMap(msResponse -> {
            if (msResponse.getCode().equals("1")) {
              //cobrar comision
              //devolver mensaje del servicio de cobro de comision
              return chargeComission(numAccount, bank.getCreditPayComission());
            } else {
              return Mono.error(new Exception("Error en la transaccion"));
            }
          });
        } else {
          return Mono.error(new Exception("el producto pertenece a este banco"));
        }
      });
    }).switchIfEmpty(Mono.error(new Exception("banco no encontrado")));
  }

}