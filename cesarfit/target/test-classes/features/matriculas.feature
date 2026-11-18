# language: pt
Funcionalidade: Matriculas com planos Mensal e Anual
  Somente dois planos existem: Mensal (R$ 120,00) e Anual (R$ 1320,00)

  Cenario: Matricular no plano Mensal
    Dado um aluno chamado "Hugo Paiva" com CPF "60670780899"
    Quando eu matricular o aluno no plano "MENSAL"
    Entao o valor da matricula deve ser "R$ 120,00"
    E o dashboard deve contar a matricula como ativa

  Cenario: Matricular no plano Anual
    Dado um aluno chamado "Iris Campos" com CPF "70780890900"
    Quando eu matricular o aluno no plano "ANUAL"
    Entao o valor da matricula deve ser "R$ 1320,00"
    E o filtro por plano "ANUAL" deve encontrar a matricula

  Cenario: Trocar de Mensal para Anual recalcula e salva o valor
    Dado um aluno chamado "Joao Brito" com CPF "80890901011"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu alterar o plano da matricula para "ANUAL"
    Entao o valor da matricula deve ser "R$ 1320,00"
    E o valor salvo no banco deve ser "R$ 1320,00"

  Cenario: Trocar de Anual para Mensal recalcula e salva o valor
    Dado um aluno chamado "Karen Dias" com CPF "90901011122"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu alterar o plano da matricula para "MENSAL"
    Entao o valor salvo no banco deve ser "R$ 120,00"

  Cenario: Plano anual nao existe mais
    Dado um aluno chamado "Lucas Reis" com CPF "01011121233"
    Quando eu tentar matricular o aluno no plano "SEMANAL"
    Entao devo receber o erro "Plano invalido: SEMANAL. Os planos disponiveis sao Mensal e Anual"

  Cenario: Nao permitir duas matriculas ativas
    Dado um aluno chamado "Mara Luz" com CPF "12123234345"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu tentar matricular o aluno no plano "ANUAL"
    Entao devo receber o erro "Mara Luz ja possui matricula ativa. Edite a matricula existente para trocar de plano."

  Cenario: Alterar a situacao da matricula
    Dado um aluno chamado "Nina Prado" com CPF "23234345456"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu alterar a situacao da matricula para "VENCIDA"
    Entao o status da matricula deve ser "VENCIDA"

  Cenario: Excluir matricula
    Dado um aluno chamado "Otto Lins" com CPF "34345456567"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu excluir a matricula
    Entao a matricula nao deve mais aparecer na listagem

  Cenario: Pagamento abaixo do valor do plano e recusado
    Dado um aluno chamado "Paula Sa" com CPF "45456567678"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu registrar um pagamento de "50.00" via "PIX"
    Entao devo receber o erro "Pagamento inferior ao valor do plano: R$ 120,00"

  # ---------- Pagamentos: forma, troco, atraso e parcelamento ----------

  Cenario: Pagamento eletronico deve ser exato
    Dado um aluno chamado "Vera Lago" com CPF "62611111101"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu registrar um pagamento de "150,00" via "PIX"
    Entao devo receber o erro "Pagamento por PIX deve ser exatamente R$ 120,00; apenas dinheiro aceita troco"

  Cenario: Pagamento em dinheiro calcula o troco
    Dado um aluno chamado "Ugo Mattos" com CPF "62611111102"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu registrar um pagamento de "150,00" via "DINHEIRO"
    Entao o ultimo pagamento deve ter troco de "R$ 30,00"
    E o total pago da matricula deve ser "R$ 120,00"

  Cenario: Pagamento em atraso cobra multa e juros e renova a vigencia
    Dado um aluno chamado "Tina Rangel" com CPF "62611111103"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando chegar o dia 10 apos o fim da matricula
    E eu registrar um pagamento de "120,00" via "PIX"
    Entao devo receber o erro "Pagamento inferior ao valor devido: R$ 122,80 (inclui multa de R$ 2,40 e juros de R$ 0,40)"
    Quando eu registrar um pagamento de "122,80" via "PIX"
    Entao a vigencia deve ter sido renovada a partir do vencimento anterior
    E o status da matricula deve ser "ATIVA"

  Cenario: Parcelamento so no plano Anual
    Dado um aluno chamado "Saulo Neri" com CPF "62611111104"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu registrar um pagamento de "120,00" via "CARTAO_CREDITO" em 3 vezes
    Entao devo receber o erro "Parcelamento disponivel apenas no plano Anual"

  Cenario: Parcelamento so no cartao de credito
    Dado um aluno chamado "Rui Ortega" com CPF "62611111105"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu registrar um pagamento de "1320,00" via "PIX" em 12 vezes
    Entao devo receber o erro "Parcelamento apenas no cartao de credito"

  Cenario: Plano anual parcelado em 12 vezes no credito
    Dado um aluno chamado "Quel Prado" com CPF "62611111106"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu registrar um pagamento de "1320,00" via "CARTAO_CREDITO" em 12 vezes
    Entao o ultimo pagamento deve ter 12 parcelas

  Cenario: Forma de pagamento inexistente
    Dado um aluno chamado "Paco Quadros" com CPF "62611111107"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu registrar um pagamento de "120,00" via "CHEQUE"
    Entao devo receber o erro "Forma de pagamento invalida: CHEQUE. Use PIX, DINHEIRO, CARTAO_DEBITO, CARTAO_CREDITO ou BOLETO"

  # ---------- Troca de plano com credito pro-rata ----------

  Cenario: Na troca de plano o valor ja pago vira credito
    Dado um aluno chamado "Olga Rios" com CPF "62611111108"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando eu alterar o plano da matricula para "ANUAL"
    Entao o credito de troca deve ser "R$ 120,00"
    E o valor devido deve ser "R$ 1200,00"
    Quando eu registrar um pagamento de "1200,00" via "CARTAO_CREDITO" em 10 vezes
    Entao a matricula nao deve ter pagamento pendente

  # ---------- Trancamento ----------

  Cenario: Somente o plano Anual pode ser trancado
    Dado um aluno chamado "Nelo Souto" com CPF "62611111109"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu tentar trancar a matricula por 15 dias
    Entao devo receber o erro "Trancamento disponivel apenas no plano Anual"

  Cenario: Trancamento estende a vigencia e bloqueia o check-in
    Dado um aluno anual pago chamado "Mila Torres" com CPF "62611111110"
    Quando eu trancar a matricula por 30 dias
    Entao o fim da matricula deve ter sido estendido em 30 dias
    Quando o aluno tentar fazer o check-in
    Entao devo receber um erro contendo "check-in indisponivel"

  Cenario: Trancamento minimo de 7 dias
    Dado um aluno anual pago chamado "Lino Ulhoa" com CPF "62611111111"
    Quando eu tentar trancar a matricula por 5 dias
    Entao devo receber o erro "O trancamento minimo e de 7 dias"

  Cenario: No maximo 60 dias de trancamento por contrato
    Dado um aluno anual pago chamado "Kati Vidal" com CPF "62611111112"
    E eu trancar a matricula por 40 dias
    E se passarem 40 dias
    Quando eu tentar trancar a matricula por 30 dias
    Entao devo receber o erro "Limite de 60 dias de trancamento por contrato: restam 20 dias"

  Cenario: No maximo 2 trancamentos por contrato
    Dado um aluno anual pago chamado "Jair Xisto" com CPF "62611111113"
    E eu trancar a matricula por 10 dias
    E se passarem 10 dias
    E eu trancar a matricula por 10 dias
    E se passarem 10 dias
    Quando eu tentar trancar a matricula por 10 dias
    Entao devo receber o erro "Limite de 2 trancamentos por contrato atingido"

  Cenario: Destrancar antes do previsto devolve os dias nao usados
    Dado um aluno anual pago chamado "Ivo Zanon" com CPF "62611111114"
    E eu trancar a matricula por 30 dias
    E se passarem 10 dias
    Quando eu destrancar a matricula
    Entao o fim da matricula deve ter sido estendido em 10 dias

  Cenario: Matricula com pagamento pendente nao pode ser trancada
    Dado um aluno chamado "Hana Abreu" com CPF "62611111115"
    E que o aluno ja foi matriculado no plano "ANUAL"
    E se passarem 6 dias
    Quando eu tentar trancar a matricula por 15 dias
    Entao devo receber o erro "Regularize o pagamento antes de trancar a matricula"
