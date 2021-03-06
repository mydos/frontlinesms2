package frontlinesms2.message

import frontlinesms2.*
import frontlinesms2.popup.*

class MessageDeleteSpec extends grails.plugin.geb.GebSpec {
	def setup() {
		createTestData()
	}

	def 'delete button does not show up for messages in trash view'() {
		when:
			remote {
				def message = Fmessage.findBySrc('Bob')
				new TrashService().sendToTrash(message)
				null
			}
			to PageMessageTrash
			messageList.clickLink()
		then:
			waitFor { singleMessageDetails.displayed }
			remote { Fmessage.deleted(false).count() == 1 }
			singleMessageDetails.sender == 'Bob'
			!singleMessageDetails.delete.displayed
	}

	def 'empty trash on confirmation deletes all trashed messages permanently and redirects to inbox'() {
		given:
			remote {
				def message = Fmessage.build()
				new TrashService().sendToTrash(message)
				assert Fmessage.findAllByIsDeleted(true).size == 1
				null
			}
			to PageMessageTrash
		when:
			trashMoreActions.value("empty-trash")
		then:
			waitFor { at DeleteDialog }
		when:
			done.click()
		then:
			waitFor { at PageMessageInbox }
			remote { Fmessage.findAllByIsDeleted(true).size } == 0
	}
	
	def "'Delete All' button appears for multiple selected messages and works"() {
		when:
			to PageMessageInbox
			messageList.toggleSelect(0)
			messageList.toggleSelect(1)
		then:
			waitFor { multipleMessageDetails.checkedMessageCount == 2 }
			multipleMessageDetails.deleteAll.click()
		then:
			waitFor { notifications.flashMessageText.contains("trash") }
	}
	
	def "'Delete' button appears for individual messages and works"() {
		when:
			to PageMessageInbox, remote { Fmessage.findBySrc('Bob').id }
		then:
			waitFor { singleMessageDetails.displayed }
			waitFor { singleMessageDetails.delete.displayed }
		when:
			singleMessageDetails.delete.click()
		then:
			at PageMessageInbox
			waitFor{ notifications.flashMessageText.contains("trash") }
	}

	static createTestData() {
		remote {
			Fmessage.build(src:'Bob', text:'hi Bob')
			Fmessage.build(src:'Alice', text:'hi Alice')
			Fmessage.build(src:'+254778899', text:'test')
			Fmessage.build(src:'Mary', text:'hi Mary')
			Fmessage.build(src:'+254445566', text:'test')
			null
		}
	}
}



