package frontlinesms2

class SmartGroupController {
	private static final String CUSTOM_FIELD_ID_PREFIX = 'custom:'
	
	def create = {
		def smartGroupInstance = new SmartGroup()
		def customFieldNames = CustomField.allUniquelyNamed
		[smartGroupInstance:smartGroupInstance,
				fieldNames:['Phone number', 'Contact name', 'email', 'notes']+customFieldNames,
				fieldIds:['mobile', 'contactName', 'email', 'notes']+customFieldNames.collect { CUSTOM_FIELD_ID_PREFIX+it }]
	}
	
	def save = {
		println "controller save smartgroup"
		def smartGroupInstance = new SmartGroup()
		smartGroupInstance.name = params.smartgroupname
		addSmartGroupRules(smartGroupInstance)
		
		if(smartGroupInstance.save()) {
			println "smargtgroup successfully saved"
			flash.message = "Created new smart group: '$params.smartgroupname'"
			redirect controller:'contact', action:'show'
		} else {
			println "smargroup save failed. Errors were $smartGroupInstance.errors"
			flash.error = "Smart group save failed. Errors were $smartGroupInstance.errors"
			render text: "Failed to save smart group<br/><br/>with params $params<br/><br/>errors: $smartGroupInstance.errors"
		}
	}
	
	def update = {
		withSmartGroup { smartGroupInstance ->
			if(params.smartgroupname)
				smartGroupInstance.name = params.smartgroupname
			if((getRuleText().flatten() - null)) {
				addSmartGroupRules(smartGroupInstance)
			}
			if(smartGroupInstance.validate()) {
				smartGroupInstance.save(failOnError: true, flush: true)
				flash.message = "Smart Group updated successfully"
				redirect(controller: "contact", action: "show", params:[smartGroupId : smartGroupInstance.id])
			}
			else {
				flash.message = "Smart Group not saved successfully"
				redirect(controller:"contact")
			}
		}
	}
	
	def show = {
		redirect(controller: "contact", action: "show", params:[smartGroupId:params.id])
	}
	
	def rename = {
		render view: "../group/rename", model: [groupName: SmartGroup.get(params.groupId)?.name]
	}

	def confirmDelete = {
		render view: "../group/confirmDelete", model: [groupName: SmartGroup.get(params.groupId)?.name]
	}
	
	def delete = {
		if (SmartGroup.get(params.id)?.delete(flush: true))
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'smartgroup.label', default: 'SmartGroup'), ''])}"
		else
			flash.message = "Unable to delete smartgroup"
		redirect(controller: "contact")
	}
	
	private def getRuleText() {
		def t = params['rule-text']
		t instanceof List? t: [t]
	}
	
	private def getRuleField(i) {
		def f = params['rule-field']
		if(f instanceof List) return f[i]
		else {
			assert i == 0
			return f
		}
	}
	
	private def addSmartGroupRules(smartGroupInstance) {
		getRuleText()?.eachWithIndex { ruleText, i ->
			def ruleField = getRuleField(i)
			if(ruleField.startsWith(CUSTOM_FIELD_ID_PREFIX)) {
				ruleField = ruleField.replaceFirst(CUSTOM_FIELD_ID_PREFIX, '')
				smartGroupInstance.addToCustomFields(new CustomField(name:ruleField, value:ruleText))
			} else {
				assert ruleField in ['contactName', 'mobile', 'email', 'notes'] // prevent injection - these should match the sanctioned fields user can set
				smartGroupInstance."$ruleField" = ruleText
			}
		}
	}
	
	private def withSmartGroup(Closure c) {
		def sg = SmartGroup.get(params.id)
		if(sg) {
			c.call(sg)
		}
		else render(text: "Could not find smartgroup with id ${params.id}")
	}
}
