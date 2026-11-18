package school.cesar.gymflow.presentation.rest;

import java.time.LocalDate;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.application.usecase.NotificacaoDeVencimentoUseCase;
import school.cesar.gymflow.application.usecase.OcorrenciaDeEquipamentoUseCase;

/** Operacao da academia (equipe): avisos de vencimento e ocorrencias de equipamentos. */
@RestController
@RequestMapping("/api")
public class OperacaoController {

    private final NotificacaoDeVencimentoUseCase notificacoes;
    private final OcorrenciaDeEquipamentoUseCase ocorrencias;

    public OperacaoController(NotificacaoDeVencimentoUseCase notificacoes, OcorrenciaDeEquipamentoUseCase ocorrencias) {
        this.notificacoes = notificacoes;
        this.ocorrencias = ocorrencias;
    }

    @GetMapping("/notificacoes")
    public List<Respostas.NotificacaoResumo> notificacoes() {
        return notificacoes.todas();
    }

    @PostMapping("/notificacoes/verificacao")
    public Respostas.VerificacaoDeVencimentos verificar() {
        return notificacoes.verificar(LocalDate.now());
    }

    @GetMapping("/ocorrencias")
    public List<Respostas.OcorrenciaResumo> ocorrencias() {
        return ocorrencias.listar();
    }

    @PostMapping("/ocorrencias")
    public ResponseEntity<Respostas.OcorrenciaResumo> registrar(@RequestBody Comandos.RegistrarOcorrencia comando) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ocorrencias.registrar(comando));
    }

    @PostMapping("/ocorrencias/{id}/andamento")
    public Respostas.OcorrenciaResumo iniciar(@PathVariable Long id) {
        return ocorrencias.iniciar(id);
    }

    @PostMapping("/ocorrencias/{id}/aguardando-peca")
    public Respostas.OcorrenciaResumo aguardarPeca(@PathVariable Long id, @RequestBody Comandos.Texto corpo) {
        return ocorrencias.aguardarPeca(id, corpo == null ? null : corpo.texto());
    }

    @PostMapping("/ocorrencias/{id}/retomada")
    public Respostas.OcorrenciaResumo retomar(@PathVariable Long id) {
        return ocorrencias.retomar(id);
    }

    /** Aceita {"solucao": "...", "custo": "45,90"} ou o formato antigo {"texto": "..."}. */
    @PostMapping("/ocorrencias/{id}/resolucao")
    public Respostas.OcorrenciaResumo resolver(@PathVariable Long id, @RequestBody java.util.Map<String, String> corpo) {
        String solucao = corpo.getOrDefault("solucao", corpo.get("texto"));
        return ocorrencias.resolver(id, new Comandos.ResolverOcorrencia(solucao, corpo.get("custo")));
    }

    @PostMapping("/ocorrencias/{id}/reabertura")
    public Respostas.OcorrenciaResumo reabrir(@PathVariable Long id, @RequestBody Comandos.Texto corpo) {
        return ocorrencias.reabrir(id, corpo == null ? null : corpo.texto());
    }
}
