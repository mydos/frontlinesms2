import grails.util.Environment
import frontlinesms2.*

class BootStrap {

	def init = { servletContext ->
		if (Environment.current == Environment.DEVELOPMENT) {
          	// do custom init for dev here
			createContact("Alice", "+123456789")
			createContact("Bob", "+198765432")
			['Friends', 'Listeners'].each() { createGroup(it) }
			Contact.findAll().each() { Group.findByName('Friends').addToMembers(it) }
			
			createFconnection("fcon", "fake", "imaps://imap.zoho.com:993?username=mr.testy@zoho.com&password=mister&debugMode=true&consumer.delay=15000")
		}
	}

	def createGroup(String n) {
		new Group(name: n).save(failOnError: true)
	}

	def createContact(String n, String a) {
		def c = new Contact(name: n, address: a)
		c.save(failOnError: true)
	}
	
	def createFconnection(String n, String t, String address) {
		new Fconnection(name: n, type: t, camelAddress: address).save(failOnError: true)
	}

	def destroy = {
	}
}
