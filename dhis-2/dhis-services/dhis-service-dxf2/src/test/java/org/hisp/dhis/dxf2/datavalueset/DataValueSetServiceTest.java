package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.time.DateUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.*;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueAuditBatchHandler;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.mock.batchhandler.MockBatchHandler;
import org.hisp.dhis.mock.batchhandler.MockBatchHandlerFactory;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class DataValueSetServiceTest
    extends DhisSpringTest
{
    private String ATTRIBUTE_UID = "uh6H2ff562G";

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private UserService _userService;

    private Attribute attribute;

    private CategoryOptionCombo ocDef;
    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;
    private Category categoryA;
    private CategoryCombo categoryComboDef;
    private CategoryCombo categoryComboA;
    private CategoryOptionCombo ocA;
    private CategoryOptionCombo ocB;

    private OptionSet osA;
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;
    private DataElement deF;
    private DataElement deG;
    private DataSet dsA;
    private DataSet dsB;
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private Period peA;
    private Period peB;
    private Period peC;

    private User user;

    private InputStream in;

    private MockBatchHandler<DataValue> mockDataValueBatchHandler = null;
    private MockBatchHandler<DataValueAudit> mockDataValueAuditBatchHandler = null;
    private MockBatchHandlerFactory mockBatchHandlerFactory = null;

    @Override
    public void setUpTest()
    {
        userService = _userService;

        mockDataValueBatchHandler = new MockBatchHandler<>();
        mockDataValueAuditBatchHandler = new MockBatchHandler<>();
        mockBatchHandlerFactory = new MockBatchHandlerFactory();
        mockBatchHandlerFactory.registerBatchHandler( DataValueBatchHandler.class, mockDataValueBatchHandler );
        mockBatchHandlerFactory.registerBatchHandler( DataValueAuditBatchHandler.class, mockDataValueAuditBatchHandler );
        setDependency( dataValueSetService, "batchHandlerFactory", mockBatchHandlerFactory );

        attribute = new Attribute( "CUSTOM_ID", ValueType.TEXT );
        attribute.setUid( ATTRIBUTE_UID );
        attribute.setUnique( true );
        attribute.setOrganisationUnitAttribute( true );
        attribute.setDataElementAttribute( true );
        idObjectManager.save( attribute );

        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );
        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );
        categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryComboDef = categoryService.getDefaultCategoryCombo();

        ocDef = categoryService.getDefaultCategoryOptionCombo();
        ocDef.setCode( "OC_DEF_CODE" );
        categoryService.updateCategoryOptionCombo( ocDef );

        osA = new OptionSet( "OptionSetA", ValueType.INTEGER );
        osA.getOptions().add( new Option( "Blue", "1" ) );
        osA.getOptions().add( new Option( "Green", "2" ) );
        osA.getOptions().add( new Option( "Yellow", "3" ) );

        ocA = createCategoryOptionCombo( categoryComboA, categoryOptionA );
        ocB = createCategoryOptionCombo( categoryComboA, categoryOptionB );
        deA = createDataElement( 'A', categoryComboDef );
        deB = createDataElement( 'B', categoryComboDef );
        deC = createDataElement( 'C', categoryComboDef );
        deD = createDataElement( 'D', categoryComboDef );
        deE = createDataElement( 'E' );
        deE.setOptionSet( osA );
        deF = createDataElement( 'F', categoryComboDef );
        deF.setValueType( ValueType.BOOLEAN );
        deG = createDataElement( 'G', categoryComboDef );
        deG.setValueType( ValueType.TRUE_ONLY );
        dsA = createDataSet( 'A', new MonthlyPeriodType() );
        dsA.setCategoryCombo( categoryComboDef );

        dsB = createDataSet( 'B' );
        dsB.setCategoryCombo( categoryComboDef );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 1, 1 ), getDate( 2012, 1, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 2, 1 ), getDate( 2012, 2, 29 ) );
        peC = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 3, 1 ), getDate( 2012, 3, 31 ) );

        ocA.setUid( "kjuiHgy67hg" );
        ocB.setUid( "Gad33qy67g5" );
        deA.setUid( "f7n9E0hX8qk" );
        deB.setUid( "Ix2HsbDMLea" );
        deC.setUid( "eY5ehpbEsB7" );
        deE.setUid( "jH26dja2f28" );
        deF.setUid( "jH26dja2f30" );
        deG.setUid( "jH26dja2f31" );
        dsA.setUid( "pBOMPrpg1QX" );
        ouA.setUid( "DiszpKrYNg8" );
        ouB.setUid( "BdfsJfj87js" );
        ouC.setUid( "j7Hg26FpoIa" );

        ocA.setCode( "OC_A" );
        ocB.setCode( "OC_B" );
        deA.setCode( "DE_A" );
        deB.setCode( "DE_B" );
        deC.setCode( "DE_C" );
        deD.setCode( "DE_D" );
        dsA.setCode( "DS_A" );
        ouA.setCode( "OU_A" );
        ouB.setCode( "OU_B" );
        ouC.setCode( "OU_C" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategory( categoryA );
        categoryService.addCategoryCombo( categoryComboA );

        categoryService.addCategoryOptionCombo( ocA );
        categoryService.addCategoryOptionCombo( ocB );

        attributeService.addAttributeValue( deA, createAttributeValue( attribute, "DE1" ) );

        dataElementService.addDataElement( deA );
        attributeService.addAttributeValue( deB, createAttributeValue( attribute, "DE2" ) );
        dataElementService.addDataElement( deB );
        attributeService.addAttributeValue( deC, createAttributeValue( attribute, "DE3" ) );
        dataElementService.addDataElement( deC );
        attributeService.addAttributeValue( deD, createAttributeValue( attribute, "DE4" ) );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deF );
        dataElementService.addDataElement( deG );

        idObjectManager.save( osA );

        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        dsA.addDataSetElement( deC );
        dsA.addDataSetElement( deD );

        attributeService.addAttributeValue( ouA, createAttributeValue( attribute, "OU1" ) );
        organisationUnitService.addOrganisationUnit( ouA );
        attributeService.addAttributeValue( ouB, createAttributeValue( attribute, "OU2" ) );
        organisationUnitService.addOrganisationUnit( ouB );
        attributeService.addAttributeValue( ouC, createAttributeValue( attribute, "OU3" ) );
        organisationUnitService.addOrganisationUnit( ouC );

        dsA.addOrganisationUnit( ouA );
        dsA.addOrganisationUnit( ouC );

        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
        periodService.addPeriod( peC );

        dataSetService.addDataSet( dsA );

        user = createUser( 'A' );
        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        userService.addUser( user );
        injectSecurityContext( user );

        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );

        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_READ_WRITE );
        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        _userService.addUser( user );
        CompleteDataSetRegistration completeDataSetRegistration = new CompleteDataSetRegistration(dsA, peA, ouA, categoryOptionCombo,
            getDate( 2012, 1, 9 ), "userA", new Date(), "userA", true);
        registrationService.saveCompleteDataSetRegistration(completeDataSetRegistration);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testImportDataValueSetXm()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        Collection<DataValueAudit> auditValues = mockDataValueAuditBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertEquals( "10002", ( ( List<DataValue> ) dataValues ).get( 1 ).getValue() );
        assertEquals( "10003", ( ( List<DataValue> ) dataValues ).get( 2 ).getValue() );

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA, ocDef );

        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );

        assertEquals( 0, auditValues.size() );
    }

    @Test
    public void testImportDataValueSetXmlPreheatCache()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions().setPreheatCache( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        Collection<DataValueAudit> auditValues = mockDataValueAuditBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertEquals( "10002", ( ( List<DataValue> ) dataValues ).get( 1 ).getValue() );
        assertEquals( "10003", ( ( List<DataValue> ) dataValues ).get( 2 ).getValue() );

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA, ocDef );

        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );

        assertEquals( 0, auditValues.size() );
    }

    @Test
    public void testImportDataValuesXmlWithCodeA()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetACode.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();
        Collection<DataValueAudit> auditValues = mockDataValueAuditBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA, ocDef );

        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );

        assertEquals( 0, auditValues.size() );
    }

    @Test
    public void testImportDataValuesXml()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithCodeB()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBCode.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions()
            .setIdScheme( "CODE" )
            .setDataElementIdScheme( "CODE" )
            .setOrgUnitIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithAttribute()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBAttribute.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions()
            .setIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setDataElementIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setOrgUnitIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithAttributeIdSchemeInPayload()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBAttributeIdScheme.xml" ).getInputStream();

        // Identifier schemes specified in XML message

        ImportOptions importOptions = new ImportOptions();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithAttributePreheatCacheTrue()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBAttribute.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions()
            .setPreheatCache( true )
            .setIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setDataElementIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setOrgUnitIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesXmlWithCodePreheatCacheTrue()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBCode.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions()
            .setPreheatCache( true )
            .setIdScheme( "CODE" )
            .setDataElementIdScheme( "CODE" )
            .setOrgUnitIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesCsv()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.csv" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, null, null );

        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesCsvWithoutHeader()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBNoHeader.csv" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, new ImportOptions().setFirstRowIsHeader( false ), null );

        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertImportDataValues( summary );
    }

    @Test
    public void testImportDataValuesBooleanCsv()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetBooleanTest.csv" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, null, null );
        assertEquals( summary.getConflicts().toString(), 4, summary.getConflicts().size() ); // False rows

        List<String> expectedBools = Lists.newArrayList( "true", "false" );
        List<DataValue> resultBools = mockDataValueBatchHandler.getInserts();

        for ( DataValue dataValue : resultBools )
        {
            assertTrue( expectedBools.contains( dataValue.getValue() ) );
        }
    }

    @Test
    public void testImportDataValuesXmlDryRun()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions()
            .setDryRun( true )
            .setIdScheme( "UID" )
            .setDataElementIdScheme( "UID" )
            .setOrgUnitIdScheme( "UID" );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testImportDataValuesXmlUpdatesOnly()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions().setImportStrategy( ImportStrategy.UPDATES );
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setIdScheme( "UID" );
        idSchemes.setDataElementIdScheme( "UID" );
        idSchemes.setOrgUnitIdScheme( "UID" );
        importOptions.setIdSchemes( idSchemes );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 12, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithNewPeriod()
        throws Exception
    {
        ImportSummary summary = dataValueSetService.saveDataValueSet( new ClassPathResource( "datavalueset/dataValueSetC.xml" ).getInputStream() );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 3, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithCategoryOptionComboIdScheme()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetCCode.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setCategoryOptionComboIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );
        assertEquals( 3, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 0, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetD.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocA ) ) );
    }

    @Test
    public void testImportDataValuesWithOrgUnitOutsideHierarchy()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetE.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
    }

    @Test
    public void testImportDataValuesWithInvalidAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetF.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithNonExistingDataElementOrgUnit()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetG.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 3, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithStrictPeriods()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictPeriods( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictCategoryOptionCombos()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictCategoryOptionCombos( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictAttributeOptionCombos()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictAttributeOptionCombos( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithRequiredCategoryOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setRequireCategoryOptionCombo( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithRequiredAttributeOptionCombo()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setRequireAttributeOptionCombo( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesWithStrictOrganisationUnits()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetNonStrict.xml" ).getInputStream();

        ImportOptions options = new ImportOptions().setStrictOrganisationUnits( true );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesInvalidOptionCode()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetInvalid.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    public void testImportDataValuesInvalidAttributeOptionComboDates()
        throws Exception
    {
        categoryOptionA.setStartDate( peB.getStartDate() );
        categoryOptionA.setEndDate( peB.getEndDate() );

        categoryService.updateCategoryOption( categoryOptionA );

        in = new ClassPathResource( "datavalueset/dataValueSetH.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocA ) ) );
    }

    @Test
    public void testImportDataValuesInvalidAttributeOptionComboOrgUnit()
        throws Exception
    {
        categoryOptionA.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );

        categoryService.updateCategoryOption( categoryOptionA );

        in = new ClassPathResource( "datavalueset/dataValueSetH.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 1, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocA ) ) );
    }

    @Test
    public void testImportDataValuesUpdatedAudit()
        throws Exception
    {
        mockDataValueBatchHandler.withFindSelf( true );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getUpdates();
        Collection<DataValueAudit> auditValues = mockDataValueAuditBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertEquals( "10002", ( ( List<DataValue> ) dataValues ).get( 1 ).getValue() );
        assertEquals( "10003", ( ( List<DataValue> ) dataValues ).get( 2 ).getValue() );

        assertEquals( 3, auditValues.size() );
    }

    @Test
    public void testImportDataValuesUpdatedSkipAudit()
        throws Exception
    {
        mockDataValueBatchHandler.withFindSelf( true );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportOptions importOptions = new ImportOptions();
        importOptions.setSkipAudit( true );
        ImportSummary summary = dataValueSetService.saveDataValueSet( in, importOptions );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        assertEquals( summary.getConflicts().toString(), 0, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getUpdates();
        Collection<DataValueAudit> auditValues = mockDataValueAuditBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );

        assertEquals( 0, auditValues.size() );
    }

    @Test
    public void testImportNullDataValues()
        throws Exception
    {
        in = new ClassPathResource( "datavalueset/dataValueSetANull.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( summary.getConflicts().toString(), 2, summary.getConflicts().size() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
    }

    @Test
    public void testImportDataValuesWithDataSetAllowsPeriods()
        throws Exception
    {
        Date thisMonth = DateUtils.truncate( new Date(), Calendar.MONTH );

        dsA.setExpiryDays( 62 );
        dsA.setOpenFuturePeriods( 2 );

        dataSetService.updateDataSet( dsA );

        Period tooEarly = createMonthlyPeriod( DateUtils.addMonths( thisMonth, 4 ) );
        Period okBefore = createMonthlyPeriod( DateUtils.addMonths( thisMonth, 1 ) );
        Period okAfter = createMonthlyPeriod( DateUtils.addMonths( thisMonth, -1 ) );
        Period tooLate = createMonthlyPeriod( DateUtils.addMonths( thisMonth, -4 ) );
        Period outOfRange =  createMonthlyPeriod( DateUtils.addMonths( thisMonth, 6 ) );

        periodService.addPeriod( tooEarly );
        periodService.addPeriod( okBefore );
        periodService.addPeriod( okAfter );
        periodService.addPeriod( tooLate );

        String importData =
            "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\" idScheme=\"code\" dataSet=\"DS_A\" orgUnit=\"OU_A\">\n" +
            "  <dataValue dataElement=\"DE_A\" period=\"" + tooEarly.getIsoDate() + "\" value=\"10001\" />\n" +
            "  <dataValue dataElement=\"DE_B\" period=\"" + okBefore.getIsoDate() + "\" value=\"10002\" />\n" +
            "  <dataValue dataElement=\"DE_C\" period=\"" + okAfter.getIsoDate() + "\" value=\"10003\" />\n" +
            "  <dataValue dataElement=\"DE_D\" period=\"" + tooLate.getIsoDate() + "\" value=\"10004\" />\n" +
            "  <dataValue dataElement=\"DE_D\" period=\"" + outOfRange.getIsoDate() + "\" value=\"10005\" />\n" +
            "</dataValueSet>\n";

        in = new ByteArrayInputStream( importData.getBytes( StandardCharsets.UTF_8 ) );

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( summary.getConflicts().toString(), 3, summary.getConflicts().size() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 3, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deB, okBefore, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, okAfter, ouA, ocDef, ocDef ) ) );
    }

    /**
     * User does not have data write access for DataSet
     * Expect fail on data sharing check
     * @throws IOException
     */
    @Test
    public void testImportValueDataSetWriteFail() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.READ );

        dataSetService.updateDataSet( dsB );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet
     * DataValue use default category combo
     * Expect success
     * @throws IOException
     */
    @Test
    public void testImportValueDefaultCatComboOk() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        dataSetService.updateDataSet( dsA );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet
     * and data read access for categoryOptions
     * Expect fail
     * @throws IOException
     */
    @Test
    public void testImportValueCatComboFail() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );

        enableDataSharing( user, categoryOptionA, AccessStringHelper.READ );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.READ );

        in = new ClassPathResource( "datavalueset/dataValueSetACatCombo.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet
     * and also categoryOptions
     * Expect success
     * @throws IOException
     */
    @Test
    public void testImportValueCatComboOk() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );

        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_WRITE );

        in = new ClassPathResource( "datavalueset/dataValueSetACatCombo.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    /**
     * User does not have data write access for DataSet
     * Expect fail
     * @throws IOException
     */
    @Test
    public void testImportValueCatComboFailDS() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ );

        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_WRITE );

        in = new ClassPathResource( "datavalueset/dataValueSetACatCombo.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet and CategoryOption
     * @throws IOException
     */
    @Test
    public void testImportValueCategoryOptionWriteOk() throws IOException
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );

        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_READ_WRITE );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }


    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertImportDataValues( ImportSummary summary )
    {
        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );

        Collection<DataValue> dataValues = mockDataValueBatchHandler.getInserts();

        assertNotNull( dataValues );
        assertEquals( 12, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouB, ocDef, ocDef ) ) );
    }

    private Period createMonthlyPeriod( Date monthStart )
    {
        Date monthEnd = DateUtils.addDays( DateUtils.addMonths( monthStart, 1 ), -1 );

        return createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), monthStart, monthEnd );
    }
}
