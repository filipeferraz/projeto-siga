package controllers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Query;

import models.GcAcesso;
import models.GcArquivo;
import models.GcInformacao;
import models.GcMovimentacao;
import models.GcTag;
import models.GcTipoInformacao;
import models.GcTipoMovimentacao;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.LocalDate;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

import play.Play;
import play.Play.Mode;
import play.data.Upload;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Catch;
import play.mvc.Http;
import play.mvc.Router;
import utils.GcArvore;
import utils.GcBL;
import utils.GcGraficoEvolucao;
import utils.GcGraficoEvolucaoItem;
import utils.GcInformacaoFiltro;
import utils.diff_match_patch;
import utils.diff_match_patch.Diff;
import utils.diff_match_patch.Operation;
import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.base.ConexaoHTTP;
import br.gov.jfrj.siga.cp.CpIdentidade;
import br.gov.jfrj.siga.dp.CpMarcador;
import br.gov.jfrj.siga.dp.CpOrgaoUsuario;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.model.dao.ModeloDao;

//Obtaining Hibernate objects programmatically
//
//You can always get access to a Hibernate Session object from an EntityManager instance through the standard EntityManager.getDelegate() method. This is a JPA specification feature.
//
//  @PersistenceContext EntityManager manager;
//  ...
//{      org.hibernate.Session session = (Session)manager.getDelegate();  }
//The specification, however, does not provide a way to get at the underlying implementation of a Query. Hibernate should provide most of its extended functionality through JPA query hints. For example, lets say you wanted to enable a query cache:
//
//  javax.persistence.Query query = manager.createQuery(...);
//  query.setHint("org.hibernate.cacheable", true);

public class Application extends SigaApplication {

	private static final String HTTP_LOCALHOST_8080 = "http://localhost:8080";

	@Before
	public static void addDefaultsAlways() throws Exception {
		prepararSessao();
		// TAH: Copiar essa classe e fazer as alterações necessárias
		// SrConfiguracaoBL.get().limparCacheSeNecessario();
	}

	@Before
	public static void addDefaults() throws Exception {

		try {
			obterCabecalhoEUsuario("#f1f4e2");
			assertAcesso("");
		} catch (Exception e) {
			tratarExcecoes(e);
		}

		try {
			assertAcesso("ADM:Administrar");
			renderArgs.put("exibirMenuAdministrar", true);
		} catch (Exception e) {
			renderArgs.put("exibirMenuAdministrar", false);
		}
	}

	static void delme_addDefaults() throws Exception {
		ModeloDao.freeInstance();

		if (request.url.contains("proxy"))
			return;

		renderArgs.put("_base", HTTP_LOCALHOST_8080);

		HashMap<String, String> atributos = new HashMap<String, String>();
		for (Http.Header h : request.headers.values())
			if (!h.name.equals("content-type"))
				atributos.put(h.name, h.value());

		String paginaVazia = ConexaoHTTP.get(HTTP_LOCALHOST_8080
				+ "/siga/pagina_vazia.action?popup=false", atributos);

		String[] pageText = paginaVazia.split("<!-- insert body -->");
		String[] cabecalho = pageText[0].split("<!-- insert menu -->");
		renderArgs.put("_cabecalho_pre", cabecalho[0]);
		renderArgs.put("_cabecalho_pos", cabecalho[1]);
		// renderArgs.put("_cabecalho", pageText[0]);
		renderArgs.put("_rodape", pageText[1]);

		String[] IDs = ConexaoHTTP.get(
				HTTP_LOCALHOST_8080 + "/siga/usuario_autenticado.action",
				atributos).split(";");

		// DpPessoa cadastrante = (DpPessoa) JPA.em().find(DpPessoa.class,
		// Long.parseLong(IDs[0]));
		DpPessoa cadastrante = DpPessoa.findById(Long.parseLong(IDs[0]));
		renderArgs.put("cadastrante", cadastrante);

		if (IDs[1] != null && !IDs[1].equals("")) {
			// DpLotacao lotaCadastrante = (DpLotacao) JPA.em().find(
			// DpLotacao.class, Long.parseLong(IDs[1]));
			DpLotacao lotaCadastrante = DpLotacao.findById(Long
					.parseLong(IDs[1]));
			renderArgs.put("lotaCadastrante", lotaCadastrante);
		}

		if (IDs[2] != null && !IDs[2].equals("")) {
			// DpPessoa titular = (DpPessoa) JPA.em().find(DpPessoa.class,
			// Long.parseLong(IDs[2]));
			DpPessoa titular = DpPessoa.findById(Long.parseLong(IDs[2]));
			renderArgs.put("titular", titular);
		}

		if (IDs[3] != null && !IDs[3].equals("")) {
			// DpLotacao lotaTitular = JPA.em().find(DpLotacao.class,
			// Long.parseLong(IDs[3]));
			DpLotacao lotaTitular = DpLotacao.findById(Long.parseLong(IDs[3]));
			// lotaTitular = (DpLotacao) GcDao.getImplementation(lotaTitular);
			renderArgs.put("lotaTitular", lotaTitular);
		}

		if (IDs[4] != null && !IDs[4].equals("")) {
			CpIdentidade identidadeCadastrante = JPA.em().find(
					CpIdentidade.class, Long.parseLong(IDs[4]));
			renderArgs.put("identidadeCadastrante", identidadeCadastrante);
		}

		int a = 0;

		if (Play.mode == Mode.DEV && GcInformacao.count() == 0) {
			Date dt = GcBL.dt();
			CpIdentidade idc = (CpIdentidade) renderArgs
					.get("identidadeCadastrante");
			DpPessoa pessoa = (DpPessoa) renderArgs.get("cadastrante");

			GcArquivo arq = new GcArquivo();
			arq.setConteudoTXT("teste 123");
			arq.titulo = "teste";

			GcMovimentacao mov = new GcMovimentacao();
			mov.arq = arq;
			mov.hisDtIni = dt;
			mov.hisIdcIni = idc;
			mov.tipo = GcTipoMovimentacao
					.findById(GcTipoMovimentacao.TIPO_MOVIMENTACAO_EDICAO);
			mov.pessoa = pessoa;
			mov.lotacao = mov.pessoa.getLotacao();

			GcInformacao inf = new GcInformacao();
			inf.autor = pessoa;
			inf.lotacao = inf.autor.getLotacao();
			inf.hisDtIni = dt;
			inf.hisIdcIni = idc;
			inf.tipo = GcTipoInformacao.findById(1L);
			inf.ou = inf.autor.getOrgaoUsuario();
			inf.movs = new TreeSet<GcMovimentacao>();
			inf.movs.add(mov);

			GcBL.gravar(inf, idc);
		}

		// } catch (Exception e) {
		// redirect("http://localhost:8080/siga");
		// }

	}

	public static void gadget() {
		Query query = JPA.em().createNamedQuery("contarGcMarcas");
		query.setParameter("idPessoaIni",
				((DpPessoa) renderArgs.get("cadastrante")).getIdInicial());
		query.setParameter("idLotacaoIni",
				((DpLotacao) renderArgs.get("lotaTitular")).getIdInicial());
		List contagens = query.getResultList();
		render(contagens);
	}

	public static void knowledge(String[] tags, String estilo, String msgvazio,
			String urlvazio, String titulo) throws Exception {
		Set<GcTag> set = GcBL.buscarTags(tags, true);
		Query query = JPA.em().createNamedQuery("buscarConhecimento");
		query.setParameter("tags", set);
		List<Object[]> conhecimentos = query.getResultList();
		for (Object[] o : conhecimentos) {
			if (o[2] != null && o[2] instanceof byte[]) {
				String s = new String((byte[]) o[2], Charset.forName("utf-8"));
				s = GcBL.ellipsize(s, 100);
				o[2] = s;
			}
		}

		if (conhecimentos.size() == 1 && "inplace".equals(estilo)) {
			GcInformacao inf = GcInformacao.findById(conhecimentos.get(0)[0]);
			conhecimentos.get(0)[1] = inf.arq.titulo;
			conhecimentos.get(0)[2] = inf.getConteudoHTML();
		}

		if (conhecimentos.size() == 0)
			conhecimentos = null;

		String referer = null;
		try {
			referer = request.headers.get("referer").value();
		} catch (Exception e) {

		}

		String classificacao = "";
		if (tags != null && tags.length > 0) {
			for (String s : tags) {
				if (classificacao.length() > 0)
					classificacao += ", ";
				classificacao += s;
			}
		}

		// if (msgvazio != null) {
		// msgvazio = msgvazio.replace("*aqui*", "<a href=\"" + urlvazio +
		// "\">aqui</a>");
		// }

		if (estilo != null)
			render("@knowledge_" + estilo, conhecimentos, classificacao,
					msgvazio, urlvazio, titulo, referer);
		else
			render(conhecimentos, classificacao, msgvazio, urlvazio, titulo,
					referer);
	}

	public static void index() {
		listar(null);
	}

	public static void top10() {
		List<GcInformacao> lista = GcInformacao.all().fetch();

		Query query1 = JPA.em().createNamedQuery("maisRecentes");
		query1.setMaxResults(5);
		List<Object[]> listaMaisRecentes = query1.getResultList();
		if (listaMaisRecentes.size() == 0)
			listaMaisRecentes = null;

		Query query2 = JPA.em().createNamedQuery("maisVisitados");
		query2.setMaxResults(5);
		List<Object[]> listaMaisVisitados = query2.getResultList();
		if (listaMaisVisitados.size() == 0)
			listaMaisVisitados = null;

		Query query3 = JPA.em().createNamedQuery("principaisAutores");
		query3.setMaxResults(5);
		List<Object[]> listaPrincipaisAutores = query3.getResultList();
		if (listaPrincipaisAutores.size() == 0)
			listaPrincipaisAutores = null;

		Query query4 = JPA.em().createNamedQuery("principaisLotacoes");
		query4.setMaxResults(5);
		List<Object[]> listaPrincipaisLotacoes = query4.getResultList();
		if (listaPrincipaisLotacoes.size() == 0)
			listaPrincipaisLotacoes = null;

		Cloud cloud = null;

		Query query5 = JPA.em().createNamedQuery("principaisTags");
		query5.setMaxResults(50);
		List<Object[]> listaPrincipaisTags = query5.getResultList();
		if (listaPrincipaisTags.size() == 0)
			listaPrincipaisTags = null;
		else {
			cloud = new Cloud(); // create cloud
			cloud.setMaxWeight(150.0); // max font size
			cloud.setMinWeight(50.0);
			double d = listaPrincipaisTags.size();
			HashMap<String, Object> map = new HashMap<String, Object>();
			for (GcTag t : (List<GcTag>) (Object) listaPrincipaisTags) {
				map.clear();
				map.put("filtro.tag", t.id);
				String link = Router.reverse("Application.listar", map).url;
				Tag tag = new Tag(t.titulo, link, d);
				cloud.addTag(tag);
				d -= 1;
			}
		}

		GcGraficoEvolucao set = new GcGraficoEvolucao();
		Query query6 = JPA.em().createNamedQuery("evolucaoNovos");
		List<Object[]> listaNovos = query6.getResultList();
		for (Object[] novos : listaNovos) {
			set.add(new GcGraficoEvolucaoItem((Integer) novos[0],
					(Integer) novos[1], (Long) novos[2], 0, 0));
		}

		Query query7 = JPA.em().createNamedQuery("evolucaoVisitados");
		List<Object[]> listaVisitados = query7.getResultList();
		for (Object[] visitados : listaVisitados) {
			set.add(new GcGraficoEvolucaoItem((Integer) visitados[0],
					(Integer) visitados[1], 0, (Long) visitados[2], 0));
		}

		LocalDate ld = new LocalDate();
		ld = new LocalDate(ld.getYear(), ld.getMonthOfYear(), 1);

		// Header
		StringBuilder sb = new StringBuilder();
		sb.append("['Mês','Visitas','Novos'],");

		// Values
		for (int i = -6; i <= 0; i++) {
			LocalDate ldl = ld.plusMonths(i);
			sb.append("['");
			sb.append(ldl.toString("MMM/yy"));
			sb.append("',");
			long novos = 0;
			long visitados = 0;
			GcGraficoEvolucaoItem o = new GcGraficoEvolucaoItem(
					ldl.getMonthOfYear(), ldl.getYear(), 0, 0, 0);
			if (set.contains(o)) {
				o = set.floor(o);
				novos = o.novos;
				visitados = o.visitados;
			}
			sb.append(visitados);
			sb.append(",");
			sb.append(novos);
			sb.append(",");
			sb.append("],");
		}
		String evolucao = sb.toString();

		render(lista, listaMaisRecentes, listaMaisVisitados,
				listaPrincipaisAutores, listaPrincipaisLotacoes,
				listaPrincipaisTags, cloud, evolucao);
	}

	public static void listar(GcInformacaoFiltro filtro) {
		List<GcInformacao> lista = filtro.buscar();

		// Montando o filtro...
		String[] tipos = new String[] { "Pessoa", "Lotação" };
		List<CpMarcador> marcadores = JPA.em()
				.createQuery("select distinct cpMarcador from GcMarca")
				.getResultList();

		List<CpOrgaoUsuario> orgaosusuarios = CpOrgaoUsuario.all().fetch();

		List<GcTipoInformacao> tiposinformacao = GcTipoInformacao.all().fetch();

		List<Integer> anos = new ArrayList<Integer>();
		int ano = GcBL.dt().getYear() + 1900;
		for (int i = 0; i < 10; i++) {
			anos.add(ano - i);
		}

		if (filtro == null)
			filtro = new GcInformacaoFiltro();

		render(lista, tipos, marcadores, filtro, orgaosusuarios,
				tiposinformacao, anos);
	}

	public static void navegar() {
		GcArvore arvore = new GcArvore();

		List<GcInformacao> infs = GcInformacao.all().fetch();

		for (GcInformacao inf : infs) {
			for (GcTag tag : inf.tags) {
				arvore.add(tag, inf);
			}
		}

		arvore.build();

		render(arvore);
	}

	public static void buscar(String texto) {
		GcArvore arvore = new GcArvore();
		List<GcInformacao> infs = GcInformacao.all().fetch();

		if (texto != null && texto.trim().length() > 0) {
			texto = texto.trim().toLowerCase();
			texto = texto.replace("  ", " ");
			String[] palavras = texto.split(" ");

			List<GcInformacao> infsFiltrada = new ArrayList<GcInformacao>();

			for (GcInformacao inf : infs) {
				if (inf.fts(palavras))
					infsFiltrada.add(inf);
			}

			infs = infsFiltrada;
		}

		for (GcInformacao inf : infs) {
			for (GcTag tag : inf.tags) {
				arvore.add(tag, inf);
			}
		}

		arvore.build();

		render(arvore, texto);
	}

	// public static void listar() {
	// List<GcInformacao> informacoes = GcInformacao.all().fetch();
	// render(informacoes);
	// }

	public static void exibir(String sigla) throws Exception {
		
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);

		if (informacao == null)
			index();

		if (!informacao.acessoPermitido(titular(), lotaTitular())) {
			throw new AplicacaoException(
					"O usuário corrente não tem acesso à informação solicitada.");
		}

		GcBL.notificado(informacao, idc());
		GcBL.logarVisita(informacao, idc());
		render(informacao);
	}

	public static void editar(String sigla, String classificacao, String titulo,
			String origem) throws IOException {
		GcInformacao informacao = new GcInformacao();

		if(sigla != null)
			informacao = informacao.findBySigla(sigla);
		else
			informacao = new GcInformacao();
		List<GcInformacao> tiposInformacao = GcTipoInformacao.all().fetch();
		List<GcAcesso> acessos = GcAcesso.all().fetch();
		if (titulo == null)
			titulo = (informacao.arq != null) ? informacao.arq.titulo : null;
		String conteudo = (informacao.arq != null) ? informacao.arq
				.getConteudoTXT() : null;
		if (classificacao == null)
			classificacao = (informacao.arq != null) ? informacao.arq.classificacao
					: null;

		if (informacao.autor == null) {
			informacao.autor = titular();
		}
		if (informacao.lotacao == null) {
			informacao.lotacao = lotaTitular();
		}

		render(informacao, tiposInformacao, acessos, titulo, conteudo,
				classificacao, origem);
	}

	public static void historico(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);

		if (informacao == null)
			index();

		diff_match_patch diff = new diff_match_patch();

		String txtAnterior = "";
		String tituloAnterior = "";

		SortedSet<GcMovimentacao> list = new TreeSet<GcMovimentacao>();
		HashMap<GcMovimentacao, String> mapTitulo = new HashMap<GcMovimentacao, String>();
		HashMap<GcMovimentacao, String> mapTxt = new HashMap<GcMovimentacao, String>();
		if (informacao.movs != null) {
			GcMovimentacao[] array = informacao.movs
					.toArray(new GcMovimentacao[informacao.movs.size()]);
			ArrayUtils.reverse(array);
			for (GcMovimentacao mov : array) {
				Long t = mov.tipo.id;

				if (mov.isCancelada())
					continue;

				if (t == GcTipoMovimentacao.TIPO_MOVIMENTACAO_EDICAO) {
					// Titulo
					String titulo = mov.arq.titulo;
					LinkedList<Diff> tituloDiffs = diff.diff_main(
							tituloAnterior, titulo, true);
					String tituloDiffHtml = diff.diff_prettyHtml(tituloDiffs);
					boolean tituloAlterado = tituloDiffs == null
							|| tituloDiffs.size() != 1
							|| tituloDiffs.size() == 1
							&& tituloDiffs.get(0).operation != Operation.EQUAL;

					// Texto
					String txt = mov.arq.getConteudoTXT();
					LinkedList<Diff> txtDiffs = diff.diff_main(txtAnterior,
							txt, true);
					String txtDiffHtml = diff.diff_prettyHtml(txtDiffs);
					boolean txtAlterado = txtDiffs == null
							|| txtDiffs.size() != 1 || txtDiffs.size() == 1
							&& txtDiffs.get(0).operation != Operation.EQUAL;

					if (tituloAlterado || txtAlterado) {
						list.add(mov);
						if (tituloAlterado)
							mapTitulo.put(mov, tituloDiffHtml);
						if (txtAlterado)
							mapTxt.put(mov, txtDiffHtml);
					}
					txtAnterior = txt;
					tituloAnterior = titulo;
				}
			}
		}

		render(informacao, list, mapTitulo, mapTxt);
	}

	public static void movimentacoes(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);

		if (informacao == null)
			index();

		render(informacao);
	}

	public static void fechar(String sigla) throws Exception {
		GcInformacao inf = new GcInformacao().findBySigla(sigla);
		GcBL.movimentar(inf, GcTipoMovimentacao.TIPO_MOVIMENTACAO_FECHAMENTO,
				null, null, null, null, null, null, null, null, null);
		GcBL.gravar(inf, idc());
		exibir(sigla);
	}

	public static void gravar(GcInformacao informacao, String titulo,
			String conteudo, String classificacao, String origem)
			throws Exception {
		DpPessoa pessoa = (DpPessoa) renderArgs.get("cadastrante");
		GcArquivo arq = new GcArquivo();

		arq = Util.marcarLinkNoConteudo(conteudo, classificacao);
		
		//Atualiza o conteudo e a classificação após a verificação se existe links criados
		conteudo = arq.getConteudoTXT();
		classificacao = arq.classificacao;
		
		if (informacao.autor == null) {
			informacao.autor = pessoa;
			informacao.lotacao = informacao.autor.getLotacao();
		}
		if (informacao.ou == null) {
			if (informacao.autor != null)
				informacao.ou = informacao.autor.getOrgaoUsuario();
			else if (informacao.lotacao != null)
				informacao.ou = informacao.lotacao.getOrgaoUsuario();
			else if (pessoa != null)
				informacao.ou = pessoa.getOrgaoUsuario();
		}
		if (informacao.tipo == null)
			informacao.tipo = GcTipoInformacao.all().first();

		// if (informacao.id != 0)
		GcBL.movimentar(informacao,
				GcTipoMovimentacao.TIPO_MOVIMENTACAO_EDICAO, pessoa,
				pessoa.getLotacao(), titulo, conteudo, classificacao, null,
				null, null, null);

		GcBL.gravar(informacao, idc());
		if (origem != null && origem.trim().length() != 0) {
			if (informacao.podeFinalizar()) {
				GcBL.movimentar(informacao,
						GcTipoMovimentacao.TIPO_MOVIMENTACAO_FECHAMENTO, null,
						null, null, null, null, null, null, null, null);
				GcBL.gravar(informacao, idc());
			}
			redirect(origem);
		} else
			exibir(informacao.getSigla());
	}

	public static void remover(String sigla) throws AplicacaoException {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);

		if (informacao.elaboracaoFim != null)
			throw new AplicacaoException(
					"Não é permitido remover informações que já foram finalizadas.");
		JPA.em().createQuery("delete from GcMarca where inf.id = :id")
				.setParameter("id", informacao.id).executeUpdate();
		JPA.em().createQuery("delete from GcMovimentacao where inf.id = :id")
				.setParameter("id", informacao.id).executeUpdate();
		informacao.delete();
		index();
	}

	public static void notificar(String sigla) {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		render(informacao);
	}

	public static void notificarGravar(GcInformacao informacao, Long pessoa,
			Long lotacao) throws Exception {
		DpPessoa pes = (DpPessoa) ((pessoa != null) ? DpPessoa.findById(pessoa)
				: null);
		DpLotacao lot = (DpLotacao) ((lotacao != null) ? DpLotacao
				.findById(lotacao) : null);
		GcBL.movimentar(informacao,
				GcTipoMovimentacao.TIPO_MOVIMENTACAO_NOTIFICAR, pes, lot, null,
				null, null, null, null, null, null);
		GcBL.gravar(informacao, idc());
		exibir(informacao.getSigla());
	}

	public static void solicitarRevisao(String sigla) {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		render(informacao);
	}

	public static void solicitarRevisaoGravar(GcInformacao informacao,
			Long pessoa, Long lotacao) throws Exception {
		DpPessoa pes = (DpPessoa) ((pessoa != null) ? DpPessoa.findById(pessoa)
				: null);
		DpLotacao lot = (DpLotacao) ((lotacao != null) ? DpLotacao
				.findById(lotacao) : null);
		GcBL.movimentar(informacao,
				GcTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_DE_REVISAO, pes,
				lot, null, null, null, null, null, null, null);
		GcBL.gravar(informacao, idc());
		exibir(informacao.getSigla());
	}

	public static void anexar(String sigla) {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		render(informacao);
	}

	public static void anexarGravar(GcInformacao informacao, Long pessoa,
			Long lotacao, String titulo, File fake) throws Exception {

		List<Upload> files = (List<Upload>) request.args.get("__UPLOADS");
		if (files != null) {
			for (Upload file : files) {
				if (file.getSize() > 0) {
					if (file.getContentType() != null) {
						String mimeType = file.getContentType().toLowerCase();
						byte anexo[] = file.asBytes();
						titulo = file.getFileName();
						DpPessoa pes = (DpPessoa) ((pessoa != null) ? DpPessoa
								.findById(pessoa) : null);
						DpLotacao lot = (DpLotacao) ((lotacao != null) ? DpLotacao
								.findById(lotacao) : null);
						GcBL.movimentar(
								informacao,
								GcTipoMovimentacao.TIPO_MOVIMENTACAO_ANEXAR_ARQUIVO,
								pes, lot, titulo, null, null, null, null,
								anexo, mimeType);
						GcBL.gravar(informacao, idc());
						exibir(informacao.getSigla());
					}
				}
			}
		}
	}

	public static void baixar(Long id) {
		GcArquivo arq = GcArquivo.findById(id);
		if (arq != null)
			renderBinary(new ByteArrayInputStream(arq.conteudo), arq.titulo,
					(long) arq.conteudo.length, arq.mimeType, true);
	}

	public static void revisado(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		if (informacao.movs != null) {
			for (GcMovimentacao mov : informacao.movs) {
				if (mov.isCancelada())
					continue;
				if (mov.tipo.id == GcTipoMovimentacao.TIPO_MOVIMENTACAO_PEDIDO_DE_REVISAO
						&& idc().getDpPessoa().equivale(mov.pessoa)) {
					GcBL.movimentar(informacao,
							GcTipoMovimentacao.TIPO_MOVIMENTACAO_REVISADO,
							mov.pessoa, mov.lotacao, null, null, null, mov,
							null, null, null);
					GcBL.gravar(informacao, idc());
					exibir(sigla);
				}
			}
		}
		throw new AplicacaoException("Não há pedido de revisão pendente para "
				+ idc().getDpPessoa().getSigla());
	}

	public static void marcarComoInteressado(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		GcBL.interessado(informacao, idc(), titular(), true);
		exibir(sigla);
	}

	public static void desmarcarComoInteressado(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		GcBL.interessado(informacao, idc(), titular(), false);
		exibir(sigla);
	}

	public static void cancelar(String sigla) throws Exception {
		GcInformacao informacao = new GcInformacao().findBySigla(sigla);
		GcBL.cancelar(informacao, idc(), titular(), lotaTitular());
		exibir(sigla);
	}

	public static void selecionarTag(String sigla) throws Exception {
		GcTag sel = (GcTag) new GcTag().selecionar(sigla);
		render("@siga-play-module.selecionar", sel);
	}

	public static void buscarTag(String sigla, GcTag filtro) {
		List<GcTag> itens = null;

		Query query = JPA.em().createNamedQuery("listarTagCategorias");
		List<Object[]> listaTagCategorias = query.getResultList();
		if (listaTagCategorias.size() == 0)
			listaTagCategorias = null;

		try {
			if (filtro == null)
				filtro = new GcTag();
			if (sigla != null && !sigla.trim().equals(""))
				filtro.setSigla(sigla);
			itens = (List<GcTag>) filtro.buscar();
		} catch (Exception e) {
			itens = new ArrayList<GcTag>();
		}

		render(itens, filtro, listaTagCategorias);
	}

	public static void proxy(String url) throws Exception {
		URL u = new URL(url);
		URLConnection conn = u.openConnection();
		for (Http.Header h : request.headers.values())
			conn.setRequestProperty(h.name, h.value());

		// BufferedReader in = new BufferedReader(new InputStreamReader(u
		// .openStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String inputLine;
		StringBuilder sb = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			sb.append(inputLine);
		}
		in.close();

		renderHtml(sb.toString());

		// byte ba[] = inputLine.getBytes("utf-8");
		// response.contentType = "text/html";
		// response.out.write(ba);
		// response.headers.put("Content-Length", new Http.Header(
		// "Content-Length", Integer.toString(ba.length)));
	}

	public static void selecionarSiga(String sigla, String tipo, String nome)
			throws Exception {
		proxy("http://localhost:8080/siga/" + tipo + "/selecionar.action?"
				+ "propriedade=" + tipo + nome + "&sigla="
				+ URLEncoder.encode(sigla, "UTF-8"));
	}

	public static void buscarSiga(String sigla, String tipo, String nome)
			throws Exception {
		proxy("http://localhost:8080/siga/" + tipo + "/buscar.action?"
				+ "propriedade=" + tipo + nome + "&sigla="
				+ URLEncoder.encode(sigla, "UTF-8"));
	}

	public static void buscarSigaFromPopup(String tipo) throws Exception {
		String paramString = "?";
		for (String s : request.params.all().keySet())
			if (!s.equals("body"))
				paramString += s + "="
						+ URLEncoder.encode(request.params.get(s), "UTF-8")
						+ "&";
		proxy("http://localhost:8080/siga/" + tipo + "/buscar.action"
				+ paramString);
	}

	protected static void assertAcesso(String path) throws Exception {
		SigaApplication.assertAcesso("GC:Módulo de Gestão de Conhecimento"
				+ path);
	}

	public static void erro(String message, String stackTrace) {
		render(message, stackTrace);
	}

	@Catch(value = AplicacaoException.class, priority = 1)
	public static void catchError(Throwable throwable) {
		// if (Play.mode.isDev())
		// return;

		// Flash.current().clear();
		// Flash.current().put("_cabecalho_pre",
		// renderArgs.get("_cabecalho_pre"));
		// Flash.current().put("_cabecalho_pos",
		// renderArgs.get("_cabecalho_pos"));
		// Flash.current().put("_rodape", renderArgs.get("_rodape"));
		while (throwable.getMessage() == null && throwable.getCause() != null)
			throwable = throwable.getCause();
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		throwable.printStackTrace(pw);
		String stackTrace = sw.toString();
		String message = throwable.getMessage();
		if (message == null)
			message = "Nenhuma informação disponível.";
		erro(message, stackTrace);
	}

}