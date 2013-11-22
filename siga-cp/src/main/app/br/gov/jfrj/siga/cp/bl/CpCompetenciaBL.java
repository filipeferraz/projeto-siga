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
package br.gov.jfrj.siga.cp.bl;

import java.lang.reflect.Method;

import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.cp.CpIdentidade;
import br.gov.jfrj.siga.cp.CpTipoConfiguracao;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;

public class CpCompetenciaBL {

	CpConfiguracaoBL configuracaoBL;

	public CpConfiguracaoBL getConfiguracaoBL() {
		return configuracaoBL;
	}

	public void setConfiguracaoBL(CpConfiguracaoBL configuracaoBL) {
		this.configuracaoBL = configuracaoBL;
	}

	/**
	 * Retorna a subsecretaria a que uma lota��o pertence. Se a lota��o j� for
	 * uma subsecretaria (o que � verificado pelo tamanho da sigla), ela mesma �
	 * retornada. Caso contr�rio, a lota��o pai � devolvida. <b>Regra
	 * aparentemente falha</b>
	 * 
	 * @param lota
	 * @return
	 */
	public DpLotacao getSubsecretaria(DpLotacao lota) {
		if (lota == null)
			return null;
		while (!lota.isSubsecretaria() && lota.getLotacaoPai() != null) {
			lota = lota.getLotacaoPai();
		}
		return lota;
	}

	public boolean podeSimularUsuario(final DpPessoa titular,
			final DpLotacao lotaTitular) throws Exception {
		return configuracaoBL.podePorConfiguracao(titular, lotaTitular,
				CpTipoConfiguracao.TIPO_CONFIG_SIMULAR_USUARIO);
	}

	public boolean testaCompetencia(final String funcao,
			final DpPessoa titular, final DpLotacao lotaTitular) {
		final Class[] classes = new Class[] { DpPessoa.class, DpLotacao.class };
		Boolean resposta = false;
		try {
			final Method method = CpCompetenciaBL.class.getDeclaredMethod(
					"pode" + funcao.substring(0, 1).toUpperCase()
							+ funcao.substring(1), classes);
			resposta = (Boolean) method.invoke(CpCompetenciaBL.class,
					new Object[] { titular, lotaTitular });
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return resposta.booleanValue();
	}

	public boolean isIdentidadeBloqueada(CpIdentidade cpIdentidade)
			throws AplicacaoException {
		try {
			return !configuracaoBL.podePorConfiguracao(cpIdentidade,
					CpTipoConfiguracao.TIPO_CONFIG_FAZER_LOGIN);
		} catch (final Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel verificar se a identidade est� bloqueada.",
					0, e);
		}
	}

	public boolean isPessoaBloqueada(DpPessoa pes) throws AplicacaoException {
		try {
			return !configuracaoBL.podePorConfiguracao(pes,
					CpTipoConfiguracao.TIPO_CONFIG_FAZER_LOGIN);
		} catch (final Exception e) {
			throw new AplicacaoException(
					"N�o foi poss�vel verificar se a pessoa est� bloqueada.",
					0, e);
		}
	}
}
