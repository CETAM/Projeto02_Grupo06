"""
Categoria: Regras de Serviço (TC-06 a TC-12)

Mesma observação do test_banco_dados.py: os asserts checam o comportamento
CORRETO (o "Resultado Esperado" da planilha). Os achados Crítica/Alta devem
falhar hoje — é isso que confirma o bug — e passar quando forem corrigidos.
Os testes usam a aplicação real via HTTP (requer BASE_URL de pé).
"""

import time

from config import BASE_URL
from helpers import api, db


def _email_unico():
    return f"teste.{int(time.time() * 1000)}@example.com"


# ----------------------------------------------------------------------
# TC-06 — Excluir cliente com pedidos vinculados deve ser bloqueado
# ----------------------------------------------------------------------
def test_tc06_excluir_cliente_com_pedido_vinculado(session, conn, cliente_de_teste, produto_de_teste):
    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}]
    )
    assert pedido is not None, "Não foi possível criar o pedido de apoio para o teste."

    try:
        resposta = api.excluir_cliente(session, cliente_de_teste)

        assert resposta.status_code != 500, (
            f"Excluir cliente com pedido vinculado retornou HTTP 500 em vez de bloquear "
            f"a exclusão com uma mensagem amigável (TC-06). Status: {resposta.status_code}."
        )
        assert "Whitelabel Error Page" not in resposta.text, (
            "A exclusão do cliente vazou a página de erro padrão do Spring Boot "
            "(Whitelabel Error Page) para o usuário final (TC-06)."
        )
    finally:
        # Libera o vínculo para o fixture conseguir limpar o cliente depois.
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-07 — Excluir produto já vendido deve ser bloqueado
# ----------------------------------------------------------------------
def test_tc07_excluir_produto_vendido(session, conn, cliente_de_teste, produto_de_teste):
    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}]
    )
    assert pedido is not None, "Não foi possível criar o pedido de apoio para o teste."

    try:
        resposta = api.excluir_produto(session, produto_de_teste)

        assert resposta.status_code != 500, (
            f"Excluir produto com pedido associado retornou HTTP 500 em vez de bloquear "
            f"a exclusão com uma mensagem amigável (TC-07). Status: {resposta.status_code}."
        )
        assert "Whitelabel Error Page" not in resposta.text, (
            "A exclusão do produto vazou a página de erro padrão do Spring Boot "
            "(Whitelabel Error Page) para o usuário final (TC-07)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-08 — Cadastrar cliente com e-mail já usado por outro
# ----------------------------------------------------------------------
def test_tc08_cliente_email_duplicado(session, conn, cliente_de_teste):
    email_existente = db.fetch_one(
        conn, "SELECT email FROM clientes WHERE id = %s", (cliente_de_teste,)
    )["email"]

    resposta = session.post(
        f"{BASE_URL}/clientes/salvar",
        data={"nome": "Outro Nome Qualquer", "email": email_existente},
    )

    assert resposta.status_code != 500, (
        f"Cadastrar cliente com e-mail duplicado retornou HTTP 500 em vez de uma mensagem "
        f"de validação amigável (TC-08). Status: {resposta.status_code}."
    )
    assert "Whitelabel Error Page" not in resposta.text, (
        "O cadastro de cliente com e-mail duplicado vazou a Whitelabel Error Page (TC-08)."
    )


# ----------------------------------------------------------------------
# TC-09 — Cadastrar categoria com nome já usado por outra
# ----------------------------------------------------------------------
def test_tc09_categoria_nome_duplicado(session, conn, categoria_de_teste):
    nome_existente = db.fetch_one(
        conn, "SELECT nome FROM categorias WHERE id = %s", (categoria_de_teste,)
    )["nome"]

    resposta = session.post(
        f"{BASE_URL}/categorias/salvar",
        data={"nome": nome_existente, "descricao": ""},
    )

    assert resposta.status_code != 500, (
        f"Cadastrar categoria com nome duplicado retornou HTTP 500 em vez de uma mensagem "
        f"de validação amigável (TC-09). Status: {resposta.status_code}."
    )
    assert "Whitelabel Error Page" not in resposta.text, (
        "O cadastro de categoria com nome duplicado vazou a Whitelabel Error Page (TC-09)."
    )


# ----------------------------------------------------------------------
# TC-10 — Filtro de relatório com Data Inicial > Data Final
# ----------------------------------------------------------------------
def test_tc10_relatorio_vendas_periodo_intervalo_invertido(session):
    # Mensagem exata que a tela mostra tanto para "sem vendas neste período"
    # (busca legítima vazia) quanto — hoje, incorretamente — para um
    # intervalo com Data Inicial depois da Data Final (ver Relatorios/
    # vendas-periodo.html). Checar por essa frase específica evita falso
    # positivo: a palavra solta "intervalo" já aparece no subtítulo fixo da
    # página, então um teste que buscasse só por palavras-chave soltas
    # acusaria "passou" mesmo sem nenhum aviso real.
    mensagem_generica_de_vazio = "Nenhuma venda foi registrada neste período."

    resposta = session.get(
        f"{BASE_URL}/relatorios/vendas-periodo",
        params={"dataInicio": "2026-08-20", "dataFim": "2026-08-01"},
    )

    assert mensagem_generica_de_vazio not in resposta.text, (
        "Com Data Inicial posterior à Data Final, o relatório mostra a mesma mensagem "
        "genérica usada para uma busca vazia legítima ('Nenhuma venda foi registrada "
        "neste período.'), sem avisar que o intervalo está invertido (TC-10)."
    )


# ----------------------------------------------------------------------
# TC-11 (positivo/confirmação) — Reduzir quantidade de um item devolve a
# diferença ao estoque
# ----------------------------------------------------------------------
def test_tc11_editar_pedido_reduzindo_quantidade_devolve_estoque(
    session, conn, cliente_de_teste, produto_de_teste
):
    estoque_inicial = db.buscar_estoque_produto(conn, produto_de_teste)

    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 3}]
    )
    assert pedido is not None

    try:
        estoque_apos_criar = db.buscar_estoque_produto(conn, produto_de_teste)
        assert estoque_apos_criar == estoque_inicial - 3, (
            f"Esperado estoque {estoque_inicial - 3} logo após criar o pedido com "
            f"quantidade 3, mas está {estoque_apos_criar}."
        )

        api.editar_pedido(
            session,
            pedido["id"],
            cliente_de_teste,
            [{"produtoId": produto_de_teste, "quantidade": 1}],
        )

        estoque_apos_editar = db.buscar_estoque_produto(conn, produto_de_teste)
        assert estoque_apos_editar == estoque_inicial - 1, (
            f"Esperado estoque {estoque_inicial - 1} após reduzir a quantidade do item de "
            f"3 para 1 (devolvendo a diferença de 2 unidades), mas está {estoque_apos_editar} "
            f"(TC-11)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-12 — Validação de campos obrigatórios só existe no HTML, não no backend
# ----------------------------------------------------------------------
def test_tc12_backend_valida_campo_obrigatorio_ausente(session):
    # Envia o POST sem o campo "nome" (obrigatório), simulando alguém que
    # removeu o atributo "required" no DevTools ou postou direto via curl/Postman.
    resposta = session.post(
        f"{BASE_URL}/clientes/salvar",
        data={"email": _email_unico()},
    )

    assert resposta.status_code != 500, (
        f"Enviar o formulário de Cliente sem o campo obrigatório 'nome' retornou HTTP 500 "
        f"(NOT NULL do banco) em vez de uma mensagem de validação amigável do backend "
        f"(TC-12). Status: {resposta.status_code}."
    )
    assert "Whitelabel Error Page" not in resposta.text, (
        "O envio sem o campo obrigatório vazou a Whitelabel Error Page (TC-12)."
    )
