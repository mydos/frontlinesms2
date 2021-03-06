package frontlinesms2.controller

import spock.lang.*
import frontlinesms2.*
import grails.plugin.spock.*

class QuickMessageControllerISpec extends IntegrationSpec {
	def controller
	
	def setup() {
		controller = new QuickMessageController()
	}

	def 'create returns the prepopulated recipients based on params.messageIds'() {
		setup:
			def m1 = new Fmessage(src:'12345', text:'This is an incoming message', inbound:true).save(failOnError:true)
			def m2 = new Fmessage(src:'23456', text:'This is an incoming message', inbound:true).save(failOnError:true)
			controller.params.messageIds = Fmessage.list().collect{ it.id }.join(",")
		when:
			def result = controller.create()
		then:
			result['addresses'] == Fmessage.list().collect{ it.src }
	}

	//TODO Add test for prepopulating the wizard with recipients and groups
}
