package com.everis.bankms.controller;

import com.everis.bankms.dto.BankMaxTransDto;
import com.everis.bankms.dto.ClientProfilesDto;
import com.everis.bankms.dto.MessageDto;
import com.everis.bankms.model.Bank;
import com.everis.bankms.service.BankService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Api(tags = "Bank API", value = "CRUD operations for clients")
@RestController
@RequestMapping("/bank")
public class BankController {

  @Autowired
  private BankService service;

  /**
   * Controlador de bankms.
   */
  @GetMapping("/test")
  public Mono<Bank> saludo() {
    Bank hola = new Bank();
    hola.setName("BCP");
    return Mono.justOrEmpty(hola);
  }

  @ApiOperation(value = "Service used to return all clients")
  @GetMapping("/findAll")
  public Flux<Bank> findAll() {
    return service.findAll();
  }

  @ApiOperation(value = "Service used to find a bank by numId")
  @GetMapping("/find/{numId}")
  public Mono<Bank> findByNumId(@PathVariable("numId") String numId) {
    return service.findById(numId);
  }

  @ApiOperation(value = "Service used to get if a bank exists")
  @GetMapping("/exist/{numId}")
  public Mono<Boolean> existBank(@PathVariable("numId") String numId) {
    return service.existsByNumId(numId);
  }

  @ApiOperation(value = "Service used to get the client profiles of a bank")
  @GetMapping("/bankProfiles/{numId}")
  public Mono<ClientProfilesDto> bankProfiles(@PathVariable("numId") String numId) {
    return service.getClientProfiles(numId);
  }

  @ApiOperation(value = "Service used to get the comissions of a bank")
  @GetMapping("/bankComission/{numId}")
  public Mono<BankMaxTransDto> bankComission(@PathVariable("numId") String numId) {
    return service.getbankComission(numId);
  }

  /**
   * GUARDAR UN BANCO.
   */
  @ApiOperation(value = "Service used to create clients")
  @PostMapping("/save")
  public Mono<ResponseEntity<Bank>> create(@Valid @RequestBody Bank bank) {
    return service.save(bank)
        .map(c -> ResponseEntity
            .created(URI.create("/banks".concat(c.getNumId())))
            .contentType(MediaType.APPLICATION_JSON)
            .body(c));
  }

  /**
   * ACTUALIZAR UN BANCO.
   */
  @ApiOperation(value = "Service used to update a client")
  @PutMapping("/update/{id}")
  public Mono<ResponseEntity<Bank>> update(@PathVariable("id") String id, @RequestBody Bank bank) {
    return service.update(bank, id)
        .map(c -> ResponseEntity
            .created(URI.create("/banks".concat(c.getNumId())))
            .contentType(MediaType.APPLICATION_JSON)
            .body(c))
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }


  /**
   * ELIMINAR UN CLIENTE.
   */
  @ApiOperation(value = "Service used to delete a client")
  @DeleteMapping("/delete/{id}")
  public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
    return service.delete(id)
        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
        .defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
  }

  //TRANSACCION
  @ApiOperation(value = "Service used to manage money transactions of a bank product")
  @PostMapping("/transaction/{idBankOrigin}/{numAccount}")
  public Mono<MessageDto> transaction(@PathVariable("idBankOrigin") String idBankOrigin,
      @PathVariable("numAccount") String numAccount, @RequestBody double money) {
    return service.otherBankDepositRet(idBankOrigin, numAccount, money);
  }

  //PAGAR DEUDA DE CREDITO
  @ApiOperation(value = "Service used to pay credit card debt")
  @PostMapping("/payCreditDebt/{idBankOrigin}/{numAccount}/{creditNumber}")
  public Mono<MessageDto> payCreditDebt(@PathVariable("idBankOrigin") String idBankOrigin,
      @PathVariable("numAccount") String numAccount,
      @PathVariable("creditNumber") String creditNumber) {
    return service.otherBankPayCreditDebt(idBankOrigin, numAccount, creditNumber);
  }

  //PAGAR A OTRA CUENTA DE BANCO
  @ApiOperation(value = "Service used to pay another bank account")
  @PostMapping("/bankProductTransaction/{idBankOrigin}/{numAccountOrigin}/{numAccountDestination}")
  public Mono<MessageDto> bankProductTransaction(@PathVariable("idBankOrigin") String idBankOrigin,
      @PathVariable("numAccountOrigin") String numAccountOrigin,
      @PathVariable("numAccountDestination") String numAccountDestination,
      @RequestBody double money) {
    return service
        .otherBankTransaction(idBankOrigin, numAccountOrigin, numAccountDestination, money);
  }
}