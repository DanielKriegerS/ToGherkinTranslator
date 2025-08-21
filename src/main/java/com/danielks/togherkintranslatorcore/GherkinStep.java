package com.danielks.togherkintranslatorcore;

public class GherkinStep {
    String tipo;
    String descricao;
    String preRequisito;

    GherkinStep(String tipo, String descricao, String preRequisito) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.preRequisito = preRequisito;
    }

}
