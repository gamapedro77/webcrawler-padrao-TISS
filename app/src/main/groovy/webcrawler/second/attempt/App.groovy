
package webcrawler.second.attempt

import groovyx.net.http.HttpBuilder
import groovyx.net.http.optional.Download
import org.jsoup.nodes.Document
import com.opencsv.CSVWriter

class App {

    static void main(String[] args) {

        Document home = connect('https://www.gov.br/ans/pt-br/')

        def buttonPrestadoresSaude = home.getElementById('ce89116a-3e62-47ac-9325-ebec8ea95473')
        def linkPrestadoresSaude = buttonPrestadoresSaude.select('a').first().attr('href')

        Document assuntosPrestadores = connect(linkPrestadoresSaude)

        def linkPadraoTiss = assuntosPrestadores.select('.card .govbr-card-content').first().attr('href')

        Document padraoTiss = connect(linkPadraoTiss)

        def URLs = padraoTiss.select('.callout')
        def linkList = []

        for (def url : URLs) {
            def link = url.select('a').first().attr('href')
            linkList.add(link)
        }

        getRecentTiss(linkList[0])
        getVersionHistoryTiss(linkList[1])
        getRelatedTablesANS(linkList[2])


    }



    static def getRecentTiss(String url) {
        Document padraoTiss = connect(url)

        def Tabela = padraoTiss.select('.table.table-bordered').first()
        def tabelaBody = Tabela.select('tbody').first().select('tr')
        for(def row : tabelaBody) {
            if (row.select('td').first().text().matches('Componente de Comunicação')) {
                def downloadUrl = row.select('a').first().attr('href')
                downloadFile(downloadUrl, './downloads/PadraoTiss.zip')
            }
        }

    }

    static def getVersionHistoryTiss(String url) {
        Document historico = connect(url)
        def rows = historico.getElementById('parent-fieldname-text').select('table').first().select('tbody').first().select('tr')
        def lines = []
        for(def row : rows) {
            def celulas = row.select('td')
            def Competencia = celulas.first().text()
            def Publicação = celulas[1].text()
            def InicioVigencia = celulas[2].text()
            def apartirDe = 2016

            if (Competencia.substring(4) as Integer >= apartirDe) {
                lines.add([Competencia, Publicação, InicioVigencia])
            }
        }

        new File("./downloads/HistoricoDeVersoesDosComponentesDoPadraoTISS.csv").withWriter( {fileWriter ->
            def csvFileWriter = new CSVWriter(fileWriter)
            String[] headerList = ['Competencia', "Publicação", 'Inicio de Vigencia']
            csvFileWriter.writeNext(headerList, false)
            for(String[] line : lines) {
                csvFileWriter.writeNext(line)
            }
        })
    }

    static def getRelatedTablesANS(String url) {
        Document tabelasRelacionadas = connect(url)

        String tabelaErrosAnsURL = tabelasRelacionadas.select('a.internal-link').first().attr('href')
        downloadFile(tabelaErrosAnsURL, './downloads/tabelasRelacionadasErrosANS.xlsx')
    }

    static def connect(String url) {
        Document doc = HttpBuilder.configure({
            request.headers['userAgent'] = 'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0'
            request.headers['referrer'] = 'https://www.google.com'

            request.uri = url
        }).get()

        return doc
    }

    static def downloadFile(String url, String filePath) {

        File newFile = new File(filePath)
        File file = HttpBuilder.configure({
            request.uri = url
        }).get({
            Download.toFile(delegate, newFile)
        })

        file.createNewFile()
    }
}
