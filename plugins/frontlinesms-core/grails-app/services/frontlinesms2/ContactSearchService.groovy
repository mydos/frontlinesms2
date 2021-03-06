package frontlinesms2

class ContactSearchService {
	static transactional = true
	def i18nUtilService
	
	def contactList(params) {
		def contactsSection = params?.groupId? Group.get(params.groupId): params.smartGroupId? SmartGroup.get(params.smartGroupId): null
		[contactInstanceList: getContacts(params),
				contactInstanceTotal: Contact.count(),
				contactsSection: contactsSection,
				contactsSectionContactTotal: countContacts(params)]
	}

	private def getContacts(params) {
		def searchString = getSearchString(params)
		if(params.groupId) {
			if (Group.get(params.groupId)) {
				return GroupMembership.searchForContacts(asLong(params.groupId), searchString, params.sort,
						params.max,
				                params.offset)
			}
			return []
		} else if(params.smartGroupId) {
			return SmartGroup.getMembersByNameIlike(asLong(params.smartGroupId), searchString, [max:params.max, offset:params.offset])
		}
		if(params.exclude) {
			return Contact.withCriteria {
				not {
					'in' 'id', params.exclude
				}
				or {
					ilike 'name', searchString
					ilike 'mobile', searchString
				}
				if(params.max) maxResults(params.max)
				if(params.order && params.sort) order params.sort, params.order
			}
		} else {
			return Contact.findAllByNameIlikeOrMobileIlike(searchString, searchString, params)
		}
	}
	
	private def countContacts(params) {
		def searchString = getSearchString(params)
		
		if(params.groupId) {
			if (Group.get(params.groupId)) {
				return GroupMembership.countSearchForContacts(asLong(params.groupId), searchString)
			}
			return 0
		} else if(params.smartGroupId) {
			return SmartGroup.countMembersByNameIlike(asLong(params.smartGroupId), searchString)
		}
		return Contact.countByNameIlikeOrMobileIlike(searchString, searchString)
	}
	
	private def getSearchString(params) {
		params.searchString? "%${params.searchString.toLowerCase()}%": '%'
	}
	
	private def asLong(Long v) { v }
	private def asLong(String s) { s.toLong() }
}
