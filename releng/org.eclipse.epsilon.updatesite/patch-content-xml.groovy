import org.zeroturnaround.zip.ZipUtil

File contentXml = new File("releng/org.eclipse.epsilon.updatesite/target/repository/content.xml")
def patchedContentXml = new File("releng/org.eclipse.epsilon.updatesite/target/repository/content.xml")

def document = new XmlParser().parse(contentXml)
def references = document.references[0]

for (repository in references.repository) {
	def type1Repository = references.appendNode("repository");
	type1Repository.@uri = repository.@uri
	type1Repository.@url = repository.@url
	type1Repository.@options = "1"
	type1Repository.@type = "1"
}

references.@size = (references.@size as Integer) * 2

new XmlNodePrinter(new PrintWriter(new FileWriter(patchedContentXml))).print(document)

for (repository in references.repository) {
	println repository.@uri
}

ZipUtil.pack(new File("releng/org.eclipse.epsilon.updatesite/target/repository"), new File("releng/org.eclipse.epsilon.updatesite/target/org.eclipse.epsilon.updatesite.zip"));