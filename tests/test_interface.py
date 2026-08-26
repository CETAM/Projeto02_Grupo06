"""
Categoria: Interface (TC-13 a TC-18)

Usa Selenium para os casos que dependem de renderização/JS real (TC-13,
TC-14, TC-15, TC-17) e requests puro para os que só precisam checar o corpo
da resposta HTTP (TC-16, TC-18). Mesma observação das outras categorias:
os asserts checam o comportamento CORRETO — hoje devem falhar nos achados
Alta/Média, e passar quando corrigidos.
"""

import time

import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from config import BASE_URL, RUN_DESTRUCTIVE_TESTS, SELENIUM_TIMEOUT
from helpers import api, db


def _email_unico():
    return f"teste.{int(time.time() * 1000)}@example.com"


def _marcar_pedido_como_processando(conn, pedido_id):
    """
    O formulário da aplicação nunca produz o status "Processando" (não é uma
    opção do <select>) — por isso, para reproduzir os cenários TC-13/TC-14,
    o valor é escrito direto no banco, simulando um pedido antigo (ex.: do
    seed) que já está nesse estado.
    """
    cursor = conn.cursor()
    cursor.execute("UPDATE pedidos SET status = %s WHERE id = %s", ("Processando", pedido_id))
    conn.commit()
    cursor.close()


# ----------------------------------------------------------------------
# TC-13 — Pedido "Processando" não exibe badge na listagem
# ----------------------------------------------------------------------
def test_tc13_pedido_processando_exibe_badge(session, conn, driver, cliente_de_teste, produto_de_teste):
    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}]
    )
    assert pedido is not None
    _marcar_pedido_como_processando(conn, pedido["id"])

    try:
        driver.get(f"{BASE_URL}/pedidos")
        linha = WebDriverWait(driver, SELENIUM_TIMEOUT).until(
            EC.presence_of_element_located(
                (By.CSS_SELECTOR, f'tr.linha-pedido[data-numero="{pedido["id"]}"]')
            )
        )
        celulas = linha.find_elements(By.TAG_NAME, "td")
        # Colunas: Nº, Cliente, Data do pedido, Itens, Valor total, Status, Ações
        celula_status = celulas[5]

        assert celula_status.text.strip() != "", (
            f"A célula de Status do pedido #{pedido['id']} está vazia para "
            f"status='Processando' — o template só cobre os 4 status conhecidos, "
            f"sem um badge padrão para status desconhecidos (TC-13)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-14 — Editar pedido "Processando" não deve trocar o status ao salvar sem alterações
# ----------------------------------------------------------------------
def test_tc14_editar_pedido_processando_mantem_status(
    session, conn, driver, cliente_de_teste, produto_de_teste
):
    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}]
    )
    assert pedido is not None
    _marcar_pedido_como_processando(conn, pedido["id"])

    try:
        driver.get(f"{BASE_URL}/pedidos")

        botao_editar = WebDriverWait(driver, SELENIUM_TIMEOUT).until(
            EC.element_to_be_clickable(
                (By.CSS_SELECTOR, f'button.btn-icon-action.edit[data-id="{pedido["id"]}"]')
            )
        )
        botao_editar.click()

        WebDriverWait(driver, SELENIUM_TIMEOUT).until(
            EC.visibility_of_element_located((By.ID, "pedidoModal"))
        )

        botao_salvar = driver.find_element(
            By.CSS_SELECTOR, "#pedidoForm button[type='submit']"
        )
        botao_salvar.click()

        # Aguarda o redirect de volta para /pedidos
        WebDriverWait(driver, SELENIUM_TIMEOUT).until(EC.url_contains("/pedidos"))

        pedido_atualizado = db.fetch_one(
            conn, "SELECT status FROM pedidos WHERE id = %s", (pedido["id"],)
        )

        assert pedido_atualizado["status"] == "Processando", (
            f"Ao editar e salvar o pedido #{pedido['id']} sem alterar nada, o status saiu "
            f"de 'Processando' para {pedido_atualizado['status']!r} — o <select> de Status "
            f"não tem a opção 'Processando', então o navegador cai na primeira opção da "
            f"lista ao tentar selecioná-la (TC-14)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-15 — Campo "Estado" deveria normalizar para maiúsculas
# ----------------------------------------------------------------------
def test_tc15_estado_normaliza_para_maiusculas(session, conn, driver):
    email = _email_unico()

    driver.get(f"{BASE_URL}/clientes")
    driver.execute_script("abrirModalNovoCliente();")

    WebDriverWait(driver, SELENIUM_TIMEOUT).until(EC.visibility_of_element_located((By.ID, "clienteModal")))

    driver.find_element(By.ID, "clienteNome").send_keys("Cliente Teste Estado")
    driver.find_element(By.ID, "clienteEmail").send_keys(email)
    driver.find_element(By.ID, "clienteEstado").send_keys("sp")

    driver.find_element(By.CSS_SELECTOR, "#clienteForm button[type='submit']").click()

    WebDriverWait(driver, SELENIUM_TIMEOUT).until(EC.url_contains("/clientes"))

    cliente_id = db.buscar_id_cliente_por_email(conn, email)
    assert cliente_id is not None, "O cliente de teste não foi encontrado no banco após salvar."

    try:
        estado_salvo = db.fetch_one(conn, "SELECT estado FROM clientes WHERE id = %s", (cliente_id,))[
            "estado"
        ]
        assert estado_salvo == "SP", (
            f"Esperado o Estado normalizado para 'SP', mas foi salvo como {estado_salvo!r} — "
            f"não há normalização client-side nem server-side (TC-15)."
        )
    finally:
        api.excluir_cliente(session, cliente_id)


# ----------------------------------------------------------------------
# TC-16 — Erros 500 não devem expor detalhes técnicos ao usuário final
# ----------------------------------------------------------------------
def test_tc16_erro_nao_expoe_detalhes_tecnicos(session, conn, cliente_de_teste):
    email_existente = db.fetch_one(
        conn, "SELECT email FROM clientes WHERE id = %s", (cliente_de_teste,)
    )["email"]

    # Reaproveita o cenário do TC-08 (e-mail duplicado) como gatilho de erro 500.
    resposta = session.post(
        f"{BASE_URL}/clientes/salvar",
        data={"nome": "Outro Nome Qualquer", "email": email_existente},
    )

    marcadores_tecnicos = [
        "Whitelabel Error Page",
        "org.springframework",
        "org.hibernate",
        "cetam.projeto02grupo06",
    ]
    vazamentos = [marcador for marcador in marcadores_tecnicos if marcador in resposta.text]

    assert not vazamentos, (
        f"A resposta de erro expôs detalhes técnicos ao usuário final (TC-16): {vazamentos}. "
        f"Não existe @ControllerAdvice nem página de erro customizada em templates/error."
    )


# ----------------------------------------------------------------------
# TC-17 (positivo/confirmação) — Botão "Novo Pedido" desabilitado sem clientes
#
# DESTRUTIVO: apaga temporariamente todos os clientes para testar o estado
# vazio, e restaura o backup ao final. Só roda com RUN_DESTRUCTIVE_TESTS=true,
# e aborta sem tocar em nada se já existir algum pedido cadastrado (evita
# violar a FK pedidos.cliente_id). Use apenas num banco de teste descartável.
# ----------------------------------------------------------------------
@pytest.mark.skipif(
    not RUN_DESTRUCTIVE_TESTS,
    reason="Defina RUN_DESTRUCTIVE_TESTS=true (num banco de teste descartável) para rodar o TC-17.",
)
def test_tc17_botao_novo_pedido_desabilitado_sem_clientes(conn, driver):
    total_pedidos = db.fetch_one(conn, "SELECT COUNT(*) AS total FROM pedidos")["total"]
    if total_pedidos > 0:
        pytest.skip(
            "Existem pedidos cadastrados — apagar os clientes quebraria a FK "
            "pedidos.cliente_id. Rode este teste só num banco sem pedidos."
        )

    backup_clientes = db.fetch_all(conn, "SELECT * FROM clientes")
    if not backup_clientes:
        pytest.skip("Não há clientes cadastrados para fazer backup/restore com segurança.")

    cursor = conn.cursor()
    try:
        cursor.execute("DELETE FROM clientes")
        conn.commit()

        driver.get(f"{BASE_URL}/pedidos")
        botao = WebDriverWait(driver, SELENIUM_TIMEOUT).until(
            EC.presence_of_element_located((By.XPATH, "//button[contains(., 'Novo Pedido')]"))
        )

        assert botao.get_attribute("disabled") is not None, (
            "O botão 'Novo Pedido' deveria estar desabilitado quando não há clientes "
            "cadastrados (TC-17)."
        )
    finally:
        colunas = list(backup_clientes[0].keys())
        placeholders = ", ".join(["%s"] * len(colunas))
        sql_restaurar = f"INSERT INTO clientes ({', '.join(colunas)}) VALUES ({placeholders})"
        for linha in backup_clientes:
            cursor.execute(sql_restaurar, tuple(linha[coluna] for coluna in colunas))
        conn.commit()
        cursor.close()


# ----------------------------------------------------------------------
# TC-18 — Mesmo produto duas vezes no payload de itens (fora da UI)
# ----------------------------------------------------------------------
def test_tc18_produto_duplicado_no_payload_nao_gera_estoque_negativo(
    session, conn, cliente_de_teste, produto_de_teste
):
    estoque_inicial = db.buscar_estoque_produto(conn, produto_de_teste)

    _, pedido = api.criar_pedido(
        session,
        conn,
        cliente_de_teste,
        [
            {"produtoId": produto_de_teste, "quantidade": 2},
            {"produtoId": produto_de_teste, "quantidade": 1},
        ],
    )
    assert pedido is not None, "Não foi possível criar o pedido com o produto duplicado no payload."

    try:
        estoque_final = db.buscar_estoque_produto(conn, produto_de_teste)

        assert estoque_final == estoque_inicial - 3, (
            f"Esperado estoque {estoque_inicial - 3} após pedido com o mesmo produto "
            f"duplicado (2 + 1 unidades), mas está {estoque_final} (TC-18)."
        )
        assert estoque_final >= 0, "O payload duplicado não deveria permitir estoque negativo (TC-18)."

        # Informativo (severidade Baixa, não falha o teste): a leitura do código indica
        # que isso gera 2 linhas separadas em itens_pedido em vez de somar numa só.
        itens = db.buscar_itens_do_pedido(conn, pedido["id"])
        print(
            f"\n[TC-18] Linhas em itens_pedido para o pedido #{pedido['id']}: {len(itens)} "
            f"(achado de baixo risco: espera-se que sejam 2 linhas separadas em vez de 1 somada)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])
