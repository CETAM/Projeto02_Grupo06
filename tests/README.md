# Testes automatizados — Sistema de Controle de Pedidos (Projeto02_Grupo06)

Suíte em **pytest**, baseada nos 18 casos de teste da planilha
`Casos_de_Teste_Projeto02_Grupo06.xlsx`. Roda contra a aplicação Spring Boot
real (`http://localhost:8080`) e consulta o MySQL (`grupo06_db`) diretamente
quando necessário.

## ⚠️ Como interpretar os resultados

Cada teste verifica o **comportamento correto** descrito na coluna "Resultado
Esperado" da planilha — não o bug. Ou seja:

- Testes que cobrem achados **Crítica / Alta / Média** devem **falhar hoje**
  (vermelho). Isso é o esperado: a falha *confirma* o achado da análise de
  código. Quando o bug for corrigido no projeto Java, o teste passa a ficar
  verde.
- Testes marcados como `(positivo/confirmação)` na planilha (TC-11, TC-17) já
  devem **passar** — eles são testes de regressão.

## 1. Instalação

```bash
pip install -r requirements.txt
```

Selenium com Edge precisa do **msedgedriver** compatível com a versão do seu
Edge instalado. A partir do Selenium 4.6+ o Selenium Manager normalmente
baixa/gerencia isso sozinho; se der erro de driver, baixe manualmente em
https://developer.microsoft.com/microsoft-edge/tools/webdriver/ e garanta que
`msedgedriver` esteja no PATH.

## 2. Banco de dados

1. Rode o script `db/grupo06_db.sql` no MySQL local (recriando o banco do
   zero) antes de rodar os testes de Banco de Dados.
2. Copie (ou aponte via `SQL_SEED_PATH`) o arquivo `grupo06_db.sql` para que
   o `test_banco_dados.py` consiga ler o TC-01 estaticamente. Por padrão ele
   procura em `../db/grupo06_db.sql` (relativo a esta pasta `testes/`).

## 3. Configuração

Tudo em `config.py` pode ser sobrescrito por variável de ambiente. As mais
importantes:

| Variável       | Padrão                    | Descrição                          |
|----------------|----------------------------|-------------------------------------|
| `BASE_URL`     | `http://localhost:8080`    | URL da aplicação Spring Boot        |
| `DB_HOST`      | `localhost`                 | Host do MySQL                       |
| `DB_PORT`      | `3306`                      | Porta do MySQL                      |
| `DB_NAME`      | `grupo06_db`                | Nome do banco                       |
| `DB_USER`      | `root`                      | Usuário do MySQL                    |
| `DB_PASSWORD`  | `12345678`                  | Senha do MySQL — **confira**: o `application.properties` do projeto usa `12345678` (8 dígitos); se a sua for diferente, exporte `DB_PASSWORD`. |
| `SQL_SEED_PATH`| `../db/grupo06_db.sql`     | Caminho do script de seed (TC-01)   |
| `BROWSER`      | `edge`                      | `edge`, `chrome` ou `firefox`       |
| `HEADLESS`     | `false`                     | `true` para rodar sem abrir janela  |
| `RUN_DESTRUCTIVE_TESTS` | `false`            | `true` para habilitar o TC-17 (apaga/restaura clientes temporariamente — só num banco descartável) |

## 4. Rodando

Com a aplicação já no ar (`http://localhost:8080`) e o banco populado:

```bash
pytest
```

Rodar só uma categoria:

```bash
pytest test_banco_dados.py
pytest test_regras_servico.py
pytest test_interface.py
```

Rodar um caso específico:

```bash
pytest test_regras_servico.py::test_tc06_excluir_cliente_com_pedido_vinculado
```

## 5. Estrutura

```
testes/
├── config.py                  # configurações (via env vars)
├── conftest.py                 # fixtures compartilhadas (session, conn, driver, dados de apoio)
├── helpers/
│   ├── api.py                  # cria/exclui Cliente, Produto, Categoria, Pedido via HTTP (form POST)
│   ├── db.py                   # conexão e queries auxiliares no MySQL
│   └── selenium_helpers.py     # setup do WebDriver
├── test_banco_dados.py         # TC-01 a TC-05
├── test_regras_servico.py      # TC-06 a TC-12
└── test_interface.py           # TC-13 a TC-18
```

## 6. Observações importantes

- Os testes que criam Cliente/Produto/Categoria/Pedido fazem isso **via
  HTTP**, pelos mesmos formulários que um usuário real usaria — não por
  INSERT direto — e limpam os registros criados ao final (`finally` /
  fixture teardown). Isso evita depender de IDs fixos do seed e evita sujar
  o banco entre execuções.
- O TC-13 e o TC-14 (status "Processando") fazem uma exceção: como o
  formulário nunca produz esse status, o teste escreve o valor direto no
  banco via `UPDATE`, simulando um pedido antigo (como os do seed) que já
  está nesse estado — reproduzindo exatamente o cenário da planilha.
- O TC-17 é **destrutivo** (precisa de um estado sem nenhum cliente
  cadastrado) e só roda com `RUN_DESTRUCTIVE_TESTS=true`; ele aborta sem
  tocar em nada se já existir algum pedido no banco, e restaura o backup dos
  clientes ao final.
