<%@ taglib tagdir="/WEB-INF/tags/mod" prefix="mod"%>
<%@ taglib uri="http://localhost/functiontag" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!-- este modelo trata de
ALTERA��O DE DADOS CADASTRAIS -->

<c:set var="esconderTexto" value="sim" scope="request" />
<mod:modelo urlBase="/paginas/expediente/modelos/requerimento_rh.jsp">	
	<mod:entrevista>	
		<mod:grupo titulo="O QUE SER� ATUALIZADO NO CADASTRO FUNCIONAL?"></mod:grupo>
		<mod:selecao titulo="Escolaridade" var="alterarEscolaridade"
			opcoes="N�o;Sim" reler="sim" />

		<mod:selecao titulo="Estado Civil" var="alterarEstadoCivil"
			opcoes="N�o;Sim" reler="sim" />

		<mod:selecao titulo="Endere�o" var="alterarEndereco" 
			opcoes="N�o;Sim" reler="sim" />

		<mod:selecao titulo="Telefone" var="alterarTelefone" 
			opcoes="N�o;Sim" reler="sim" />
		
		<hr>
		
		<mod:grupo titulo="Exemplo de Radio">			
			<mod:radio titulo="15%" var="acessorios" valor="1" marcado="Sim" />		
			<mod:radio titulo="20%" var="acessorios" valor="2" />			
			<mod:radio titulo="25%" var="acessorios" valor="3" />		
			
		</mod:grupo>	
		
		<mod:grupo>
			<mod:caixaverif titulo="C�pia autenticada de Certificado" var="objetivo1" obrigatorio="Sim"/>
		</mod:grupo>
		<mod:grupo>
			<mod:caixaverif titulo="C�pia n�o autenticada de Certificado" var="objetivo2" />
		</mod:grupo>

		<c:if test="${alterarEscolaridade == 'Sim'}">
			<mod:grupo>
				<mod:selecao titulo="Escolaridade" var="escolaridade"
					opcoes="Antigo Prim�rio;Antigo Gin�sio;Antigo Cient�fico;
				Ensino Fundamental ou 1� Grau; Ensino M�dio ou 2� grau;
				Superior ou Gradua��o; P�s-Gradua��o, Mestrado ou Doutorado"
					reler="n�o" />
			</mod:grupo>
		</c:if>

		<c:if test="${alterarEstadoCivil == 'Sim'}">
			<mod:grupo>
				<mod:selecao titulo="Estado Civil" var="estadocivil"
					opcoes="Solteiro(a);Casado(a);Divorciado(a);Desquitado(a);Vi�vo(a)"
					reler="n�o" />
			</mod:grupo>
		</c:if>

		<c:if test="${alterarEndereco == 'Sim'}">
			<mod:grupo>
				<mod:texto largura="80" titulo="Endere�o" var="endereco"/>
			</mod:grupo>
			<mod:grupo>
				<mod:texto largura="40" titulo="Bairro" var="bairro" />
				<mod:texto largura="30" titulo="Cidade" var="cidade" />
				<mod:texto largura="2" titulo="UF" var="uf" />
			</mod:grupo>
			<mod:grupo>
				<mod:texto largura="9" titulo="CEP" var="cep" />
			</mod:grupo>
			<mod:mensagem titulo="Aten��o" texto="&quot;Se comprovadamente falsa a declara��o, sujeitar-se-� o declarante �s san��es civis, administrativas e criminais previstas na legisla��o aplic�vel.&quot; (Lei 7115/1983, art. 2�)" vermelho="Sim"></mod:mensagem>
		</c:if>

		<c:if test="${alterarTelefone == 'Sim'}">
			<mod:grupo>
				<mod:texto largura="18" titulo="Telefone 1" var="tel1" />
			</mod:grupo>
			<mod:grupo>
				<mod:texto largura="18" titulo="Telefone 2" var="tel2" />
			</mod:grupo>
			<mod:grupo>
				<mod:texto largura="18" titulo="Telefone 3" var="tel3" />
			</mod:grupo>
		</c:if>

	</mod:entrevista>


	<mod:documento>
		<mod:valor var="texto_requerimento">
			
		<p style="TEXT-INDENT: 2cm" align="justify" >		
		${doc.subscritor.descricao}, ${doc.subscritor.cargo.nomeCargo}, ${doc.subscritor.padraoReferenciaInvertido}, lotado(a)
		no(a) ${doc.subscritor.lotacao.descricao}, solicita a Vossa Senhoria
		que sejam efetuadas as seguintes <b>ALTERA��ES EM SEUS ASSENTAMENTOS FUNCIONAIS</b>:
		</p>
		
		<c:if test="${acessorios == '1'}">
			<p> percentagem = 15 </p>
		</c:if>
		<c:if test="${acessorios == '2'}">
			<p> percentagem = 20 </p>
		</c:if>
		<c:if test="${acessorios == '3'}">
			<p> percentagem = 25 </p>
		</c:if>
		
		<c:if test="${objetivo1 == 'Sim'}">
			C�pia autenticada de Certificado marcado
		</c:if> <br>
		<c:if test="${objetivo2 == 'Sim'}">
			C�pia n�o autenticada de Certificado marcado
		</c:if>
		
		<ul>

		
		
		<c:if test="${alterarEscolaridade == 'Sim'}">
		<li><b>
		<u>Escolaridade:</u></b> (Com documento autenticado em anexo)<br>${escolaridade}</li>
		</c:if>
		
		
		<c:if test="${alterarEstadoCivil == 'Sim'}">
			<li><b><u>Estado Civil:</u></b> (Com documento autenticado em anexo)<br>
			${estadocivil}</li>	
		</c:if>

			
		<c:if test="${alterarEndereco == 'Sim'}">
			<li><b><u>Endereco:</u></b><br />
						${endereco}</li>
				<b>Bairro: </b> ${bairro}<br />
				<b>Cidade: </b> ${cidade}<br />
				<b>UF: </b> ${uf}<br />
				<b>CEP: </b> ${cep}<br />
				<b>Aten��o: &quot;Se comprovadamente falsa a declara��o, 
				sujeitar-se-� o declarante �s san��es civis, administrativas e criminais 
				previstas na legisla��o aplic�vel.&quot; (Lei 7115/1983, art. 2�)</b> 
				
		</c:if>
			
		
		<c:if test="${tel1 != null || tel2 != null || tel3 != null }">
			<li><b><u>TELEFONE(S)</u></b><br>
			
			
					<c:if test="${tel1 != ''}">	
					${tel1}<br>
					</c:if>	
				
					<c:if test="${tel2 != ''}">	
					${tel2}<br>
					</c:if>	
					
					<c:if test="${tel3 != ''}">					
					${tel3}<br>
					</c:if>	
		</c:if>
		</ul>
		</mod:valor>		
	</mod:documento>
	<mod:assinatura>
		{Formul�rio Assinado! Iniciando procedimento "Adicional Titulacao".}
		<c:set var="f" value="${f:criarWorkflow('Adicional Titulacao', doc, cadastrante, titular, lotaCadastrante, lotaTitular)}" />
	</mod:assinatura>
</mod:modelo>
