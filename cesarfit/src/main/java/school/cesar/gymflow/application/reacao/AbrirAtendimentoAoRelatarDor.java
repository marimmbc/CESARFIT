package school.cesar.gymflow.application.reacao;

import org.springframework.stereotype.Component;

import school.cesar.gymflow.application.usecase.AtendimentoDoInstrutorUseCase;
import school.cesar.gymflow.domain.shared.evento.ManipuladorDeEvento;
import school.cesar.gymflow.domain.treino.evento.DorRelatada;

/** Contexto Atendimento reage a Treino: dor relatada no feedback abre um chamado prioritario. */
@Component
public class AbrirAtendimentoAoRelatarDor implements ManipuladorDeEvento<DorRelatada> {

    private final AtendimentoDoInstrutorUseCase atendimentos;

    public AbrirAtendimentoAoRelatarDor(AtendimentoDoInstrutorUseCase atendimentos) {
        this.atendimentos = atendimentos;
    }

    @Override
    public Class<DorRelatada> tipo() {
        return DorRelatada.class;
    }

    @Override
    public void tratar(DorRelatada evento) {
        atendimentos.abrirAutomatico(evento.alunoId(), "Dor relatada no feedback pos-treino: " + evento.localDaDor());
    }
}
