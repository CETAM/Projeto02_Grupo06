"""
Configuração do WebDriver (Selenium) usado pelos testes de Interface.
"""

from config import BROWSER, HEADLESS


def criar_driver():
    if BROWSER == "edge":
        from selenium import webdriver
        from selenium.webdriver.edge.options import Options

        options = Options()
        if HEADLESS:
            options.add_argument("--headless=new")
        return webdriver.Edge(options=options)

    if BROWSER == "chrome":
        from selenium import webdriver
        from selenium.webdriver.chrome.options import Options

        options = Options()
        if HEADLESS:
            options.add_argument("--headless=new")
        return webdriver.Chrome(options=options)

    if BROWSER == "firefox":
        from selenium import webdriver
        from selenium.webdriver.firefox.options import Options

        options = Options()
        if HEADLESS:
            options.add_argument("--headless")
        return webdriver.Firefox(options=options)

    raise ValueError(f"Navegador não suportado em BROWSER: {BROWSER!r}")
