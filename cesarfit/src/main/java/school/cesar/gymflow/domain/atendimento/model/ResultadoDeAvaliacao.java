package school.cesar.gymflow.domain.atendimento.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/** Objeto de valor: medidas da avaliacao fisica, com IMC e classificacao (tabela da OMS). */
public record ResultadoDeAvaliacao(double pesoKg, double alturaM, Double percentualDeGordura) {

    public ResultadoDeAvaliacao {
        if (pesoKg < 30 || pesoKg > 300) {
            throw new RegraDeNegocioException("Peso deve estar entre 30 e 300 kg");
        }
        if (alturaM < 1.0 || alturaM > 2.5) {
            throw new RegraDeNegocioException("Altura deve estar entre 1,00 e 2,50 m");
        }
        if (percentualDeGordura != null && (percentualDeGordura < 3 || percentualDeGordura > 60)) {
            throw new RegraDeNegocioException("Percentual de gordura deve estar entre 3% e 60%");
        }
    }

    public BigDecimal imc() {
        return BigDecimal.valueOf(pesoKg / (alturaM * alturaM)).setScale(2, RoundingMode.HALF_UP);
    }

    public String classificacao() {
        double imc = imc().doubleValue();
        if (imc < 18.5) return "Abaixo do peso";
        if (imc < 25) return "Peso normal";
        if (imc < 30) return "Sobrepeso";
        if (imc < 35) return "Obesidade grau I";
        if (imc < 40) return "Obesidade grau II";
        return "Obesidade grau III";
    }
}
