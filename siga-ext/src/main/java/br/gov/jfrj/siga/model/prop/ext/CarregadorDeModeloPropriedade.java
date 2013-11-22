/*******************************************************************************
 * Copyright (c) 2006 - 2011 SJRJ.
 * 
 *     This file is part of SIGA.
 * 
 *     SIGA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     SIGA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with SIGA.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package br.gov.jfrj.siga.model.prop.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Classe utilit�ria para a carga das propriedades
 * 
 * @author aym
 * 
 */
public class CarregadorDeModeloPropriedade {
	private static Hashtable<String, Properties> cache = new Hashtable<String, Properties>();
	private static final String NOME_ARQ_PROPS = "siga.properties";

	/*
	 * private static final String NOME_GERENTE_RAIZ = ModeloPropriedade.class
	 * .getName();
	 */
	/**
	 * Carrega as propriedades do arquivo de propriedades
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Properties obterTodas(ModeloPropriedade obj) throws Exception {
		Properties geral = new Properties();
		Class cls = obj.getClass();
		String clsNme = cls.getName();
		// retorna propriedades da cache, se tem l�
		Properties prpCache = cache.get(clsNme);
		if (prpCache != null)
			return prpCache;
		//
		while (true) {
			Properties prp = carregarPara(cls);
			Enumeration ks = prp.keys();
			while (ks.hasMoreElements()) {
				Object k = ks.nextElement();
				if (!geral.containsKey(k)) {
					geral.put(k, prp.get(k));
				}
			}
			// verifica a superclasse se � classe PropriedadesGerais (abstrata)
			/*
			 * if (cls.getSuperclass().getName().equals(NOME_GERENTE_RAIZ))
			 * break;
			 */
			// verifica se a classe � abstrata
			if (Modifier.isAbstract(cls.getSuperclass().getModifiers()))
				break;
			cls = cls.getSuperclass();
		}
		cache.put(clsNme, geral);
		return geral;
	}

	/**
	 * retorna as propriedades para o par�metro obj (Objeto ou classe)
	 * 
	 * @param obj
	 *            uma instancia ou [nome-da-classe].class
	 * @return
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException
	 */
	private static Properties carregarPara(Object obj) throws Exception {

		// tenta carregar tentar carregar propriedades de um arquivo na no mesmo
		// pacote da classe
		try {
			return carregarPropriedadesNoProprioCodigo(getClassNameOf(obj));
		} catch (Exception e) {

		}

		// tenta carregar tentar carregar propriedades um arquivo informado na
		// linha de comando
		try {
			return carregarPropriedadesArquivoEspecifico(System
					.getProperty(NOME_ARQ_PROPS + ".file"));
		} catch (Exception e) {

		}

		// tentar carregar propriedades a partir diretorio do programa
		try {
			return carregarPropriedadesArquivoEspecifico("./" + NOME_ARQ_PROPS);
		} catch (Exception e) {

		}

		// tenta carregar carregar propriedades registradas por um servidor de
		// aplica��es, por exemplo.
		try {
			return carregarPropriedadesDoSistema();
		} catch (Exception e) {

		}

		throw new Exception(
				"N�o existem propriedades definidas! Para resolver esse problema: \n 1) Coloque um arquivo "
						+ NOME_ARQ_PROPS
						+ " no mesmo pacote da classe que estende ModeloPropriedade \n 2) Defina uma vari�vel -D"
						+ NOME_ARQ_PROPS
						+ ".file=CAMINHO_COMPLETO_DO_ARQUIVO na inicializa��o do programa \n 3) Coloque um arquivo "
						+ NOME_ARQ_PROPS
						+ " no mesmo diret�rio do aplicativo \n 4) Se for uma aplica��o web, coloque o arquivo "
						+ NOME_ARQ_PROPS
						+ " no diret�rio de configura��o do servidor de aplica��o e configure-o para ler tal arquivo");

	}

	private static Properties carregarPropriedadesDoSistema() throws Exception {
		Properties p = System.getProperties();
		if (p.containsKey("siga.properties.versao")) {
			return System.getProperties();
		} else {
			throw new Exception("Propriedades inv�lidas!");
		}
	}

	/**
	 * Tenta carregar as propriedades em um arquivo localizado em um diret�rio
	 * espec�fico. Ess forma � ideal para compartilhar arquivos de propriedades
	 * entre m�dulos web e n�o-web, mantendo-os em um arquivo �nico.
	 * 
	 * @param caminhoArquivo
	 * @return
	 * @throws java.io.IOException
	 */
	private static Properties carregarPropriedadesArquivoEspecifico(
			String caminhoArquivo) throws IOException {
		Properties p = new Properties();
		File f = new File(caminhoArquivo);
		InputStream is = new FileInputStream(f);
		p.load(is);
		is.close();
		return p;
	}

	/**
	 * Tenta carregar as propriedades em um arquivo que esteja no mesmo pacote
	 * da classe. Essa forma � ideal para o desenvolvedor que queira definir uma
	 * propriedade que s� pode ser modificada por desenvolcedores.
	 * 
	 * @param nme
	 * @return
	 * @throws ClassNotFoundException
	 * @throws java.io.IOException
	 */
	private static Properties carregarPropriedadesNoProprioCodigo(String nme)
			throws ClassNotFoundException, IOException {
		Properties p = new Properties();
		InputStream is = Class.forName(nme).getResourceAsStream(NOME_ARQ_PROPS);
		p.load(is);
		is.close();
		return p;
	}

	/**
	 * Obt�m o nome (caminho completo) da classe do objeto par�metro.
	 * 
	 * @param obj
	 *            deve ser uma instancia ou [nome-da-classe].class
	 * @return o nome (caminho completo) da classe do objeto par�metro.
	 */
	@SuppressWarnings("unchecked")
	private static String getClassNameOf(Object obj) {
		if (obj.getClass().getName().equals("java.lang.Class")) {
			return ((Class) obj).getName();
		} else {
			return obj.getClass().getName();
		}
	}

}
