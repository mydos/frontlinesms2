package frontlinesms2.contact

import frontlinesms2.*

class ContactListPage extends geb.Page {
	static url = 'contact/list'
	static at = {
		title.endsWith('Contacts')
	}
	static content = {
		selectedMenuItem { $('#contacts-menu .selected') }
		groupsList(required:false) { $('#groups-submenu') }
		contactSelect(required:false) { $(".contact-select") }	
		multiGroupSelect(required:false) { $('#multi-group-dropdown') }
		updateAll(required:false) { $("#update-all") }
		flashMessage(required:false) { $('div.flash') }
	}
}
