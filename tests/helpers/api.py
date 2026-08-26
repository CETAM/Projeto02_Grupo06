"""
Ações HTTP contra a aplicação, usando exatamente os campos de formulário
definidos nos templates Thymeleaf (não é uma API REST — os controllers
recebem POST de formulário e redirecionam para a listagem).

Como os controllers não devolvem o ID criado no corpo da resposta, cada
`criar_*` consulta o banco logo depois para descobrir o ID (por isso todo
`criar_*` recebe uma conexão de banco `conn`).
"""

import json
import time

from config import BASE_URL
from helpers import db


def _buscar_com_retry(func_busca, *args, tentativas=5, intervalo=0.2):
    """
    Repete uma consulta de "buscar ID recém-criado" algumas vezes antes de
    desistir. É uma rede de segurança contra qualquer pequena folga de
    tempo entre o commit da aplicação e a nossa consulta — na prática deve
    achar de primeira quase sempre.
    """
    resultado = None
    for _ in range(tentativas):
        resultado = func_busca(*args)
        if resultado is not None:
            return resultado
        time.sleep(intervalo)
    return resultado


# ----------------------------------------------------------------------
# Categorias
# ----------------------------------------------------------------------
def criar_categoria(session, conn, nome, descricao=""):
    resposta = session.post(
        f"{BASE_URL}/categorias/salvar",
        data={"nome": nome, "descricao": descricao},
    )
    categoria_id = _buscar_com_retry(db.buscar_id_categoria_por_nome, conn, nome)
    if categoria_id is None:
        raise AssertionError(
            f"POST /categorias/salvar não resultou numa categoria com nome={nome!r} "
            f"no banco. Status da resposta: {resposta.status_code}."
        )
    return categoria_id


def excluir_categoria(session, categoria_id):
    return session.post(f"{BASE_URL}/categorias/excluir/{categoria_id}")


# ----------------------------------------------------------------------
# Clientes
# ----------------------------------------------------------------------
def criar_cliente(session, conn, nome, email, telefone="", cidade="", estado="", ativo=True):
    data = {
        "nome": nome,
        "email": email,
        "telefone": telefone,
        "cidade": cidade,
        "estado": estado,
    }
    if ativo:
        data["ativo"] = "true"
    resposta = session.post(f"{BASE_URL}/clientes/salvar", data=data)
    cliente_id = _buscar_com_retry(db.buscar_id_cliente_por_email, conn, email)
    if cliente_id is None:
        raise AssertionError(
            f"POST /clientes/salvar não resultou num cliente com email={email!r} "
            f"no banco. Status da resposta: {resposta.status_code}."
        )
    return cliente_id


def excluir_cliente(session, cliente_id):
    return session.post(f"{BASE_URL}/clientes/excluir/{cliente_id}")


# ----------------------------------------------------------------------
# Produtos
# ----------------------------------------------------------------------
def criar_produto(session, conn, nome, preco, quantidade_estoque, categoria_id, descricao=""):
    data = {
        "nome": nome,
        "descricao": descricao,
        "preco": str(preco),
        "quantidadeEstoque": str(quantidade_estoque),
        "categoriaId": str(categoria_id),
    }
    resposta = session.post(f"{BASE_URL}/produtos/salvar", data=data)
    produto_id = _buscar_com_retry(db.buscar_id_produto_por_nome, conn, nome)
    if produto_id is None:
        raise AssertionError(
            f"POST /produtos/salvar não resultou num produto com nome={nome!r} "
            f"no banco. Status da resposta: {resposta.status_code}."
        )
    return produto_id


def excluir_produto(session, produto_id):
    return session.post(f"{BASE_URL}/produtos/excluir/{produto_id}")


# ----------------------------------------------------------------------
# Pedidos
# ----------------------------------------------------------------------
def criar_pedido(session, conn, cliente_id, itens, status="Pendente", observacoes="", data_entrega=""):
    """
    itens: lista de dicts [{"produtoId": 1, "quantidade": 2}, ...]
    (é exatamente o formato que o JS do modal monta no campo oculto itensJson)
    """
    data = {
        "clienteId": str(cliente_id),
        "status": status,
        "observacoes": observacoes,
        "dataEntrega": data_entrega,
        "itensJson": json.dumps(itens),
    }
    resposta = session.post(f"{BASE_URL}/pedidos/salvar", data=data)
    pedido = db.buscar_ultimo_pedido_do_cliente(conn, cliente_id)
    return resposta, pedido


def editar_pedido(session, pedido_id, cliente_id, itens, status="Pendente", observacoes="", data_entrega=""):
    data = {
        "id": str(pedido_id),
        "clienteId": str(cliente_id),
        "status": status,
        "observacoes": observacoes,
        "dataEntrega": data_entrega,
        "itensJson": json.dumps(itens),
    }
    return session.post(f"{BASE_URL}/pedidos/salvar", data=data)


def excluir_pedido(session, pedido_id):
    return session.post(f"{BASE_URL}/pedidos/excluir/{pedido_id}")
