<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8"
	buffer="64kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://localhost/customtag" prefix="tags"%>
<%@ taglib uri="http://jsptags.com/tags/navigation/pager" prefix="pg"%>
<%@ taglib prefix="ww" uri="/webwork"%>
<%@ taglib uri="http://localhost/functiontag" prefix="f"%>
<%@ taglib uri="http://localhost/sigatags" prefix="siga"%>


<siga:pagina titulo="Transferência em Lote">

<script type="text/javascript" language="Javascript1.1"
	src="<c:url value="/staticJavascript.action"/>"></script>

<script type="text/javascript" language="Javascript1.1">
	<ww:url id="url" action="atender_pedido_publicacao" namespace="/expediente/mov"/>
	function sbmt(offset) {
		frm.action = '${url}';
		frm.submit();
	}
	
	function checkUncheckAll(theElement) {
		var theForm = theElement.form, z = 0;
		for(z=0; z<theForm.length;z++) {
	    	if(theForm[z].type == 'checkbox' && theForm[z].name != 'checkall') {
				theForm[z].checked = !(theElement.checked);
				theForm[z].click();
			}
		}
	}
	
	function displaySel(chk, el) {
		document.getElementById('div_' + el).style.display=chk.checked ? '' : 'none';
		if (chk.checked == -2) 
			document.getElementById(el).focus();
	}
	
	function displayTxt(sel, el) {					
		document.getElementById('div_' + el).style.display=sel.value == -1 ? '' : 'none';
		document.getElementById(el).focus();
	}
	
	
</script>

	<div class="gt-bd clearfix">
		<div class="gt-content clearfix">

<ww:form name="frm" action="atender_pedido_publicacao_gravar"
	namespace="/expediente/mov" method="GET" theme="simple">
	<ww:hidden name="postback" value="1" />
	<br />
	<%--<c:forEach begin="1" end="3" var="i">--%>
	<c:set var="i" value="1" />
	<c:choose>
		<c:when test="${i == 1}">
			<h2>Documentos com solicitação de publicação</h2>
			<c:set var="elementos" value="${itensSolicitados}" />
		<div class="gt-content-box gt-for-table">
			<table class="gt-form-table">
				<tr class="header">
					<td colspan="2">Envio</td>
				</tr>
				<tr>
					<td colspan="2"><ww:submit value="Remeter para publicação" cssClass="gt-btn-large gt-btn-left"/></td>
				</tr>
			</table>
		</div>
		<br/>
			
		</c:when>
		<c:when test="${i == 2}">
			<h2>Documentos remetidos para publicação</h2>
			<c:set var="elementos" value="${itensRemetidos}" />
		</c:when>
		<c:when test="${i == 3}">
			<h2>Documentos publicados</h2>
			<c:set var="elementos" value="${itensPublicados}" />
		</c:when>
	</c:choose>
	
	<c:if test="${(not empty elementos)}">
	<div class="gt-content-box gt-for-table">
	<table class="gt-table">
		<tr class="header">
			<c:if test="${i == 1}">
				<td rowspan="2" align="center"><input type="checkbox"
					name="checkall" onclick="checkUncheckAll(this)" /></td>
			</c:if>
			<td rowspan="2" align="center">Número</td>
			<c:choose>
				<c:when test="${i == 1}">			
				<td rowspan="2" style="color:red" align="center">Data de Disponibilização</td>	
				</c:when>
				<c:when test="${i == 2}">
					<td align="center">Data de remessa</td>
				</c:when>
			</c:choose>
			<td colspan="3" align="center">Documento</td>
			<td rowspan="2">Descrição</td>
			<td rowspan="2" align="center">Tipo de Matéria</td>
			<td rowspan="2" align="center">Lotação de Publicação</td>		
		</tr>
		<tr>			
			<td align="center">Data</td>
			<td align="center">Lotação</td>
			<td align="center">Pessoa</td>	
					
		</tr>

		<c:forEach var="documento" items="${elementos}">
			<c:choose>
				<c:when test='${evenorodd == "even"}'>
					<c:set var="evenorodd" value="odd" />
				</c:when>
				<c:otherwise>
					<c:set var="evenorodd" value="even" />
				</c:otherwise>
			</c:choose>
			<tr class="${evenorodd}">
				<c:if test="${i == 1}">
					<c:set var="x" scope="request">chk_${documento.idDoc}</c:set>
					<c:remove var="x_checked" scope="request" />
					<c:if test="${param[x] == 'true'}">
						<c:set var="x_checked" scope="request">checked</c:set>
					</c:if>
					<td width="2%" align="center"><input type="checkbox"
						name="${x}" value="true" ${x_checked} /></td>
				</c:if>
				<td width="11.5%" align="right"><a href="javascript:void(0)"
							onclick="javascript: window.open('/sigaex/expediente/doc/exibir.action?popup=true&id=${documento.idDoc}', '_new', 'width=700,height=500,scrollbars=yes,resizable')">${documento.codigo}</a>
				<c:choose>
					<c:when test="${i == 1}">
						<td width="11.5%" align="center">${documento.dtDispUltimoAgendamento}</td>
					</c:when>
					<c:when test="${i == 2}">
						<td rowspan="2" width="11.5%">${documento.dtUltimaRemessaParaPublicacao}</td>
					</c:when>
				</c:choose>
				<td width="5%" align="center">${documento.dtDocDDMMYY}</td>
				<td width="4%" align="center"><siga:selecionado
					sigla="${documento.subscritor.iniciais}"
					descricao="${documento.subscritor.descricao}" /></td>
				<td width="4%" align="center"><siga:selecionado
					sigla="${documento.lotaSubscritor.sigla}"
					descricao="${documento.lotaSubscritor.descricao}" /></td>

				
				<c:forEach var="movimentacao" items="${documento.exMovimentacaoSet}">				
						<c:choose>
							<c:when test='${movimentacao.exTipoMovimentacao.idTpMov == 38 && 
							!movimentacao.cancelada}'>
							<td width="44%">${movimentacao.descrPublicacao}</td>
							<td width="2%" align="left">
								<ww:radio list="#{'J':'Judicial', 'A':'Administrativa'}" name="tpm_${documento.idDoc}" id="tm" label="Tipo de Matéria"  
								value="'${movimentacao.cadernoPublicacaoDje}'"  />								
							</td>								
							<td align="center">${movimentacao.lotaPublicacao}</td>
							</c:when>
						</c:choose>
				</c:forEach>
				
				
<%--			<td width="6%" align="center">
					<ww:url id="url" action="atender_pedido_publicacao_cancelar" namespace="/expediente/mov">
						<ww:param name="sigla">${documento.sigla}</ww:param>
					</ww:url>
					<ww:a href="%{url}">Cancelar Pedido</ww:a>					
				</td>
 --%>	
	 			<td>
	 			 	<a
						style="border-left: 0px; float: right; padding-left: 0.0em; padding-right: 0.0em;"
						href="javascript:if (confirm('Deseja excluir o pedido?')) location.href='/sigaex/expediente/mov/atender_pedido_publicacao_cancelar.action?sigla=${documento.sigla}';">
						<img
							style="display: inline;"
							src="/siga/css/famfamfam/icons/cancel_gray.png" title="Excluir pedido"							
							onmouseover="this.src='/siga/css/famfamfam/icons/cancel.png';" 
							onmouseout="this.src='/siga/css/famfamfam/icons/cancel_gray.png';"/>
					</a>
	 			</td>
	 			<td><a href="/sigaex/arquivo/download.action?arquivo=${documento.mobilGeral.referenciaRTF}">RTF</a>
	 			</td>				
			</tr>
		</c:forEach>
	</table>
	</div>
	<br />
	<%--</c:forEach>--%>
	</c:if>
</ww:form>

</div></div>
</siga:pagina>
