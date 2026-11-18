# language: pt
Funcionalidade: Gerenciamento de alunos
  Como recepcionista da academia
  Quero cadastrar, pesquisar, editar e excluir alunos

  Cenario: Pesquisar aluno cadastrado pelo nome
    Dado um aluno chamado "Carla Mendes" com CPF "10120230344"
    Entao a pesquisa por "carla" deve encontrar o aluno

  Cenario: Nao permitir CPF duplicado
    Dado um aluno chamado "Diego Alves" com CPF "20230340455"
    Quando eu tentar cadastrar um aluno com CPF "202.303.404-55"
    Entao devo receber o erro "Ja existe aluno cadastrado com o CPF 202.303.404-55"

  Cenario: Nao permitir cadastro sem telefone
    Quando eu tentar cadastrar um aluno sem telefone
    Entao devo receber o erro "Telefone invalido: informe DDD + numero (10 ou 11 digitos)"

  Cenario: Editar os dados de um aluno
    Dado um aluno chamado "Elisa Rocha" com CPF "30340450566"
    Quando eu alterar o nome do aluno para "Elisa Rocha Lima" e o telefone para "81 3333-4444"
    Entao o aluno deve se chamar "Elisa Rocha Lima" com telefone "(81) 3333-4444"

  Cenario: Excluir aluno sem matricula
    Dado um aluno chamado "Fabio Nunes" com CPF "40450560677"
    Quando eu excluir o aluno
    Entao o aluno nao deve mais aparecer na listagem

  Cenario: Bloquear exclusao de aluno com matricula ativa
    Dado um aluno chamado "Gabi Torres" com CPF "50560670788"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu tentar excluir o aluno
    Entao devo receber o erro "Nao e possivel excluir Gabi Torres: ele possui matricula ativa (Plano Mensal). Cancele ou exclua a matricula antes."

  # ---------- Regras ampliadas ----------

  Cenario: Menor de 18 anos exige responsavel legal
    Quando eu tentar cadastrar um aluno de 17 anos chamado "Theo Pires" com CPF "61611111101" sem responsavel
    Entao devo receber o erro "Aluno menor de 18 anos exige nome e CPF do responsavel legal"

  Cenario: Menor de idade cadastrado com responsavel
    Quando eu cadastrar um aluno de 16 anos chamado "Luna Pires" com CPF "61611111102" e responsavel "Marta Pires" com CPF "61611111199"
    Entao o aluno deve ser menor de idade com o responsavel "Marta Pires"

  Cenario: Responsavel nao pode ter o mesmo CPF do aluno
    Quando eu tentar cadastrar um aluno de 17 anos chamado "Davi Pires" com CPF "61611111103" e responsavel "Davi Pires" com CPF "61611111103"
    Entao devo receber o erro "O CPF do responsavel deve ser diferente do CPF do aluno"

  Cenario: E-mail nao pode se repetir entre alunos
    Dado um aluno chamado "Rosa Lemos" com CPF "61611111104"
    Quando eu tentar cadastrar outro aluno com o mesmo e-mail e CPF "61611111105"
    Entao devo receber um erro contendo "ja esta em uso por outro aluno"

  Cenario: Aluno com historico financeiro e anonimizado em vez de apagado (LGPD)
    Dado um aluno chamado "Igor Tavares" com CPF "61611111106"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E eu cancelar a matricula com a justificativa "Mudanca de cidade por trabalho"
    Quando eu excluir o aluno
    Entao o aluno nao deve mais aparecer na listagem
    E a matricula deve continuar no historico em nome de um aluno anonimizado
