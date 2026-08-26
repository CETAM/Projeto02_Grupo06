"""
Categoria: Banco de Dados (TC-01 a TC-05)

Estes testes leem o estado real do MySQL (grupo06_db). Rode o script de
seed (db/grupo06_db.sql) antes de executar este arquivo.

IMPORTANTE sobre o "resultado esperado" destes testes: cada assert abaixo
verifica o comportamento CORRETO descrito na planilha, não o bug. Ou seja,
hoje eles devem FALHAR (vermelho) — isso confirma o achado. Quando o bug for
corrigido, o teste correspondente deve passar (verde).
"""

import os

import pytest

from config import SQL_SEED_PATH
from helpers import api, db


STATUS_VALIDOS_NO_FORMULARIO = {"Pendente", "Confirmado", "Enviado", "Entregue", "Cancelado"}


# ----------------------------------------------------------------------
# TC-01 — Script de seed executa sem erro de sintaxe
# ----------------------------------------------------------------------
def test_tc01_script_seed_sem_erro_de_sintaxe():
    """
    Verificação estática: todo bloco de comentário "/* ... */" deve estar
    balanceado. Um "*/" sem abertura correspondente é erro de sintaxe SQL
    e pode interromper a execução do script (achado Crítica do TC-01).
    """
    caminho = os.path.abspath(os.path.join(os.path.dirname(__file__), SQL_SEED_PATH))

    if not os.path.exists(caminho):
        pytest.skip(
            f"db/grupo06_db.sql não encontrado em {caminho}. "
            f"Ajuste SQL_SEED_PATH no config.py ou copie o arquivo para lá."
        )

    with open(caminho, encoding="utf-8") as arquivo:
        conteudo = arquivo.read()

    dentro_de_comentario = False
    posicao = 0
    comentarios_sem_abertura = []

    while posicao < len(conteudo):
        if not dentro_de_comentario and conteudo[posicao : posicao + 2] == "/*":
            dentro_de_comentario = True
            posicao += 2
        elif dentro_de_comentario and conteudo[posicao : posicao + 2] == "*/":
            dentro_de_comentario = False
            posicao += 2
        elif not dentro_de_comentario and conteudo[posicao : posicao + 2] == "*/":
            linha = conteudo.count("\n", 0, posicao) + 1
            comentarios_sem_abertura.append(linha)
            posicao += 2
        else:
            posicao += 1

    assert not comentarios_sem_abertura, (
        f'Encontrado(s) "*/" sem "/*" de abertura correspondente na(s) linha(s) '
        f"{comentarios_sem_abertura} de {SQL_SEED_PATH} — isso quebra a execução "
        f"do script num cliente MySQL. (TC-01)"
    )


# ----------------------------------------------------------------------
# TC-02 — Soma dos itens de cada pedido deve bater com valor_total
# ----------------------------------------------------------------------
def test_tc02_soma_itens_bate_com_valor_total(conn):
    pedidos = db.fetch_all(conn, "SELECT id, valor_total FROM pedidos")
    divergencias = []

    for pedido in pedidos:
        soma = db.fetch_one(
            conn,
            "SELECT COALESCE(SUM(subtotal), 0) AS total FROM itens_pedido WHERE pedido_id = %s",
            (pedido["id"],),
        )["total"]

        if soma != pedido["valor_total"]:
            divergencias.append(
                f"pedido #{pedido['id']}: valor_total={pedido['valor_total']} "
                f"mas soma dos itens={soma}"
            )

    assert not divergencias, (
        "Pedido(s) com valor_total divergente da soma real dos itens (TC-02): "
        + "; ".join(divergencias)
    )


# ----------------------------------------------------------------------
# TC-03 — Todo pedido com valor_total > 0 deve ter ao menos 1 item vinculado
# ----------------------------------------------------------------------
def test_tc03_pedido_com_valor_tem_itens_vinculados(conn):
    linhas = db.fetch_all(
        conn,
        """
        SELECT p.id, p.valor_total, COUNT(ip.id) AS qtd_itens
        FROM pedidos p
        LEFT JOIN itens_pedido ip ON ip.pedido_id = p.id
        GROUP BY p.id, p.valor_total
        """,
    )

    sem_itens = [
        f"pedido #{linha['id']} (valor_total={linha['valor_total']})"
        for linha in linhas
        if linha["valor_total"] and linha["valor_total"] > 0 and linha["qtd_itens"] == 0
    ]

    assert not sem_itens, (
        "Pedido(s) com valor_total preenchido mas nenhum item em itens_pedido (TC-03): "
        + "; ".join(sem_itens)
    )


# ----------------------------------------------------------------------
# TC-04 — Investigação completa do campo "Previsão de entrega"
#
# Dividido em 3 testes que, juntos, provam a causa raiz:
#   (a) o trigger trg_set_data_entrega FUNCIONA — dispara e preenche a data
#       sozinho quando você insere direto na tabela pedidos;
#   (b) mas a aplicação (PedidoService.salvar) manda duas instruções SQL
#       pro mesmo pedido — um INSERT e, logo depois, um UPDATE redundante —
#       e o UPDATE reescreve data_entrega com o valor desatualizado que
#       ainda estava em memória (null), apagando o que o trigger fez;
#   (c) por isso o estado final, visto pelo usuário, é null — só que por
#       um motivo bem diferente do que a leitura superficial sugere.
# ----------------------------------------------------------------------


def test_tc04a_trigger_preenche_data_entrega_em_insert_direto(conn, cliente_de_teste):
    """
    Prova que o trigger, por si só, funciona: um INSERT feito direto na
    tabela (sem passar pela aplicação) já sai com data_entrega preenchida.
    """
    cursor = conn.cursor()
    cursor.execute(
        """
        INSERT INTO pedidos (cliente_id, status, valor_total, observacoes)
        VALUES (%s, 'Pendente', 0, 'TC-04a - insert direto para testar o trigger')
        """,
        (cliente_de_teste,),
    )
    pedido_id = cursor.lastrowid
    cursor.close()

    try:
        pedido = db.fetch_one(conn, "SELECT data_entrega FROM pedidos WHERE id = %s", (pedido_id,))

        assert pedido["data_entrega"] is not None, (
            "O trigger trg_set_data_entrega não preencheu data_entrega num INSERT feito "
            "direto na tabela — ou seja, o trigger em si não está funcionando (isso "
            "contradiz o comportamento esperado do trigger)."
        )
    finally:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM pedidos WHERE id = %s", (pedido_id,))
        cursor.close()


def test_tc04b_service_grava_pedido_com_insert_seguido_de_update_redundante(
    session, conn, cliente_de_teste, produto_de_teste
):
    """
    Prova, com evidência real do general_log do MySQL, que criar um pedido
    pela aplicação gera DUAS instruções SQL na tabela pedidos (INSERT +
    UPDATE) — e que o UPDATE final volta a gravar data_entrega = NULL,
    desfazendo o que o INSERT/trigger tinham acabado de fazer.
    """
    geral_original, saida_original = db.capturar_configuracao_log(conn)
    try:
        db.habilitar_general_log(conn)
    except Exception as erro:
        pytest.skip(
            f"Não foi possível ligar o general_log do MySQL (privilégio insuficiente?): {erro}"
        )

    try:
        inicio = db.momento_atual_do_servidor(conn)

        _, pedido = api.criar_pedido(
            session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}]
        )
        assert pedido is not None

        try:
            instrucoes = db.buscar_instrucoes_sobre_tabela(conn, "pedidos", inicio, contendo=None)
            # Mantém só INSERT/UPDATE na tabela pedidos (ignora SELECTs de leitura)
            escritas = [
                linha
                for linha in instrucoes
                if "insert into `pedidos`" in linha["argument"].lower()
                or "insert into pedidos" in linha["argument"].lower()
                or "update `pedidos`" in linha["argument"].lower()
                or "update pedidos" in linha["argument"].lower()
            ]

            texto_completo = " || ".join(linha["argument"] for linha in escritas)
            tem_insert = "insert into" in texto_completo.lower() and "pedidos" in texto_completo.lower()
            tem_update = "update" in texto_completo.lower() and "pedidos" in texto_completo.lower()

            print("\n[TC-04b] Instruções SQL capturadas na tabela pedidos:")
            for linha in escritas:
                print(f"  {linha['event_time']}  {linha['argument']}")

            assert tem_insert and tem_update, (
                "Esperava ver um INSERT seguido de um UPDATE na tabela pedidos para uma "
                "única criação de pedido (evidência do save() redundante em "
                "PedidoService.salvar), mas o general_log capturou: "
                f"{[l['argument'] for l in escritas]}"
            )

            pedido_final = db.fetch_one(
                conn, "SELECT data_entrega FROM pedidos WHERE id = %s", (pedido["id"],)
            )
            assert pedido_final["data_entrega"] is None, (
                "Esperado que o UPDATE final sobrescrevesse data_entrega de volta para NULL "
                "(a causa raiz do TC-04), mas o valor ficou preenchido — o comportamento pode "
                "ter mudado."
            )
        finally:
            api.excluir_pedido(session, pedido["id"])
    finally:
        db.restaurar_configuracao_log(conn, geral_original, saida_original)


def test_tc04c_estado_final_visto_pelo_usuario_fica_em_branco(
    session, conn, cliente_de_teste, produto_de_teste
):
    """
    O "resultado esperado" da planilha (campo em branco) — confirma o
    estado final que o usuário realmente vê, sem entrar na causa raiz
    (coberta pelos testes TC-04a/TC-04b acima).
    """
    _, pedido = api.criar_pedido(
        session, conn, cliente_de_teste, [{"produtoId": produto_de_teste, "quantidade": 1}], data_entrega=""
    )
    assert pedido is not None

    try:
        assert pedido["data_entrega"] is None, (
            "O campo 'Previsão de entrega' não ficou em branco após criar o pedido sem "
            "preenchê-lo (TC-04)."
        )
    finally:
        api.excluir_pedido(session, pedido["id"])


# ----------------------------------------------------------------------
# TC-05 — Todo status presente no banco deve existir como opção no formulário
# ----------------------------------------------------------------------
def test_tc05_status_do_banco_existem_no_formulario(conn):
    linhas = db.fetch_all(conn, "SELECT DISTINCT status FROM pedidos")
    status_no_banco = {linha["status"] for linha in linhas}

    status_orfaos = status_no_banco - STATUS_VALIDOS_NO_FORMULARIO

    assert not status_orfaos, (
        f"Status presente(s) no banco mas ausente(s) do <select> do formulário (TC-05): "
        f"{status_orfaos}. Opções do formulário: {STATUS_VALIDOS_NO_FORMULARIO}."
    )
