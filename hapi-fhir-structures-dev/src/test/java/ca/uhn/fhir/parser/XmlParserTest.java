package ca.uhn.fhir.parser;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dev.composite.DurationDt;
import ca.uhn.fhir.model.dev.resource.Encounter;
import ca.uhn.fhir.model.dev.resource.MedicationPrescription;
import ca.uhn.fhir.model.dev.resource.Organization;
import ca.uhn.fhir.model.dstu.resource.Binary;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.valueset.BundleEntryStatusEnum;

public class XmlParserTest {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(XmlParserTest.class);
	private static final FhirContext ourCtx = FhirContext.forDev();

	@BeforeClass
	public static void beforeClass() {
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void testDuration() {
		Encounter enc = new Encounter();
		DurationDt duration = new DurationDt();
		duration.setUnits("day").setValue(123L);
		enc.setLength(duration);
		
		String str = ourCtx.newXmlParser().encodeResourceToString(enc);
		ourLog.info(str);
		
		assertThat(str, not(containsString("meta")));
		assertThat(str, containsString("<length><value value=\"123\"/><units value=\"day\"/></length>"));
	}
	
	@Test
	public void testParseBundleWithBinary() {
		// TODO: implement this test, make sure we handle ID and meta correctly in Binary
	}

	@Test
	public void testParseAndEncodeBundle() throws Exception {
		String content = IOUtils.toString(XmlParserTest.class.getResourceAsStream("/bundle-example(example).xml"));

		Bundle parsed = ourCtx.newXmlParser().parseBundle(content);
		assertEquals("http://example.com/base/Bundle/example/_history/1", parsed.getId().getValue());
		assertEquals("1", parsed.getResourceMetadata().get(ResourceMetadataKeyEnum.VERSION));
		assertEquals("1", parsed.getId().getVersionIdPart());
		assertEquals(new InstantDt("2014-08-18T01:43:30Z"), parsed.getResourceMetadata().get(ResourceMetadataKeyEnum.UPDATED));
		assertEquals("transaction", parsed.getType().getValue());
		assertEquals(3, parsed.getTotalResults().getValue().intValue());
		assertEquals("http://example.com/base", parsed.getLinkBase().getValue());
		assertEquals("https://example.com/base/MedicationPrescription?patient=347&searchId=ff15fd40-ff71-4b48-b366-09c706bed9d0&page=2", parsed.getLinkNext().getValue());
		assertEquals("https://example.com/base/MedicationPrescription?patient=347", parsed.getLinkSelf().getValue());

		assertEquals(1, parsed.getEntries().size());
		assertEquals("update", parsed.getEntries().get(0).getStatus().getValue());
		assertEquals(BundleEntryStatusEnum.UPDATE, ResourceMetadataKeyEnum.ENTRY_STATUS.get(parsed.getEntries().get(0).getResource()));
		assertEquals("http://foo?search", parsed.getEntries().get(0).getLinkSearch().getValue());

		MedicationPrescription p = (MedicationPrescription) parsed.getEntries().get(0).getResource();
		assertEquals("Patient/example", p.getPatient().getReference().getValue());
		assertEquals("2014-08-16T05:31:17Z", ResourceMetadataKeyEnum.UPDATED.get(p).getValueAsString());
		assertEquals("http://example.com/base/MedicationPrescription/3123/_history/1", p.getId().getValue());

		String reencoded = ourCtx.newXmlParser().setPrettyPrint(true).encodeBundleToString(parsed);
		ourLog.info(reencoded);

		Diff d = new Diff(new StringReader(content), new StringReader(reencoded));
		assertTrue(d.toString(), d.identical());

	}

	@Test
	public void testEncodeAndParseBundleWithoutResourceIds() {
		Organization org = new Organization();
		org.addIdentifier().setSystem("urn:system").setValue("someval");
		
		Bundle bundle = Bundle.withSingleResource(org);
		String str = ourCtx.newXmlParser().encodeBundleToString(bundle);
		ourLog.info(str);
		
		Bundle parsed = ourCtx.newXmlParser().parseBundle(str);
		assertThat(parsed.getEntries().get(0).getResource().getId().getValue(), isEmptyOrNullString());
		assertTrue(parsed.getEntries().get(0).getResource().getId().isEmpty());
	}
	
	@Test
	public void testBundleWithBinary() {
		//@formatter:off
		String bundle = "<Bundle xmlns=\"http://hl7.org/fhir\">\n" + 
			"   <meta/>\n" + 
			"   <base value=\"http://localhost:52788\"/>\n" + 
			"   <total value=\"1\"/>\n" + 
			"   <link>\n" + 
			"      <relation value=\"self\"/>\n" + 
			"      <url value=\"http://localhost:52788/Binary?_pretty=true\"/>\n" + 
			"   </link>\n" + 
			"   <entry>\n" + 
			"      <resource>\n" + 
			"         <Binary xmlns=\"http://hl7.org/fhir\">\n" + 
			"            <id value=\"1\"/>\n" + 
			"            <meta/>\n" + 
			"            <contentType value=\"text/plain\"/>\n" + 
			"            <content value=\"AQIDBA==\"/>\n" + 
			"         </Binary>\n" + 
			"      </resource>\n" + 
			"   </entry>\n" + 
			"</Bundle>";
		//@formatter:on
		
		Bundle b = ourCtx.newXmlParser().parseBundle(bundle);
		assertEquals(1, b.getEntries().size());
		
		Binary bin = (Binary) b.getEntries().get(0).getResource();
		assertArrayEquals(new byte[] {1,2,3,4}, bin.getContent());
		
	}
	
	
}
