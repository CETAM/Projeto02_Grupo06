import time

import pytest
import requests

from config import BASE_URL
from helpers import db as db_helper
from helpers import api
from helpers.selenium_helpers import criar_driver


# ----------------------------------------------------------------------
# HTTP
# ----------------------------------------------------------------------
@pytest.fixture()
def session():
    """
    requests.Session isolada por teste — mantém cookies para o mecanismo de
    flash attributes (RedirectAttributes) do Spring funcionar corretamente
    entre o POST e o redirect.
    """
    s = requests.Session()
    yield s
    s.close()


# ----------------------------------------------------------------------
# Banco de dados
# ----------------------------------------------------------------------
@pytest.fixture()
def conn():
    connection = db_helper.get_connection()
    yield connection
    connection.close()


# ----------------------------------------------------------------------
# Selenium
# ----------------------------------------------------------------------
@pytest.fixture()
def driver():
    d = criar_driver()
    d.implicitly_wait(3)
    yield d
    d.quit()


# ----------------------------------------------------------------------
# Fixtures de dados — criam registros via HTTP (não via INSERT direto) para
# que os testes exerçam o mesmo caminho de código que um usuário real, e
# removem tudo ao final para não sujar o banco entre execuções.
# ----------------------------------------------------------------------
def _sufixo_unico():
    return str(int(time.time() * 1000))[-8:]


@pytest.fixture()
def categoria_de_teste(session, conn):
    nome = f"Categoria Teste {_sufixo_unico()}"
    categoria_id = api.criar_categoria(session, conn, nome=nome, descricao="Criada pela suíte de testes")
    assert categoria_id is not None, "Não foi possível criar a categoria de apoio para o teste."
    yield categoria_id
    api.excluir_categoria(session, categoria_id)


@pytest.fixture()
def cliente_de_teste(session, conn):
    sufixo = _sufixo_unico()
    email = f"teste.{sufixo}@example.com"
    cliente_id = api.criar_cliente(session, conn, nome=f"Cliente Teste {sufixo}", email=email)
    assert cliente_id is not None, "Não foi possível criar o cliente de apoio para o teste."
    yield cliente_id
    api.excluir_cliente(session, cliente_id)


@pytest.fixture()
def produto_de_teste(session, conn, categoria_de_teste):
    nome = f"Produto Teste {_sufixo_unico()}"
    produto_id = api.criar_produto(
        session, conn, nome=nome, preco="99.90", quantidade_estoque=50, categoria_id=categoria_de_teste
    )
    assert produto_id is not None, "Não foi possível criar o produto de apoio para o teste."
    yield produto_id
    api.excluir_produto(session, produto_id)


@pytest.fixture()
def app_no_ar():
    """Aborta a suíte cedo, com mensagem clara, se a aplicação não estiver de pé."""
    try:
        requests.get(BASE_URL, timeout=5)
    except requests.exceptions.ConnectionError:
        pytest.exit(
            f"Não foi possível conectar em {BASE_URL}. "
            f"A aplicação Spring Boot está rodando?"
        )
