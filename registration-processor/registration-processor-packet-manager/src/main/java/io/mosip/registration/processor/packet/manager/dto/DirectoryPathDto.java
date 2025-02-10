package io.mosip.registration.processor.packet.manager.dto;

// TODO: Auto-generated Javadoc
/**
 * The directory names for FileManager operation.
 * 
 * @author M1039303
 *
 */
public enum DirectoryPathDto {

	/** The archive location. */
	ARCHIVE_LOCATION {
		@Override
		public String toString() {
			return "registration.processor.ARCHIVE_LOCATION";
		}
	},


	/** The landing zone. */
	LANDING_ZONE {
		@Override
		public String toString() {
			return "registration.processor.LANDING_ZONE";
		}
	},

	/** The landing zone. */
	RESIDENT_PRINT_LANDING_ZONE {
		@Override
		public String toString() {
			return "registration.processor.RESIDENT_PRINT_LANDING_ZONE";
		}
	},

}
