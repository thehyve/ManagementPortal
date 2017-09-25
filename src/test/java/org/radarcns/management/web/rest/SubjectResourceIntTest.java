package org.radarcns.management.web.rest;

import org.radarcns.management.ManagementPortalApp;

import org.radarcns.management.domain.Subject;
import org.radarcns.management.repository.DeviceTypeRepository;
import org.radarcns.management.repository.ProjectRepository;
import org.radarcns.management.repository.SensorDataRepository;
import org.radarcns.management.repository.SubjectRepository;
import org.radarcns.management.service.DeviceTypeService;
import org.radarcns.management.service.SubjectService;
import org.radarcns.management.service.dto.AttributeMapDTO;
import org.radarcns.management.service.dto.ProjectDTO;
import org.radarcns.management.service.dto.SourceRegistrationDTO;
import org.radarcns.management.service.dto.SubjectDTO;
import org.radarcns.management.service.mapper.DeviceTypeMapper;
import org.radarcns.management.service.mapper.SubjectMapper;
import org.radarcns.management.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the SubjectResource REST controller.
 *
 * @see SubjectResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ManagementPortalApp.class)
public class SubjectResourceIntTest {

    private static final String DEFAULT_EXTERNAL_LINK = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_LINK = "BBBBBBBBBB";

    private static final String DEFAULT_ENTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_ENTERNAL_ID = "BBBBBBBBBB";

    private static final Boolean DEFAULT_REMOVED = false;
    private static final Boolean UPDATED_REMOVED = true;

    private static final String DEFAULT_EMAIL= "someone@gmail.com";

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @Autowired
    private DeviceTypeMapper deviceTypeMapper;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private ProjectRepository projectRepository;

    private MockMvc restSubjectMockMvc;

    private MockMvc restDeviceTypeMockMvc;

    private Subject subject;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SubjectResource subjectResource = new SubjectResource();
        ReflectionTestUtils.setField(subjectResource, "subjectService" , subjectService);
        ReflectionTestUtils.setField(subjectResource, "subjectRepository" , subjectRepository);
        ReflectionTestUtils.setField(subjectResource, "subjectMapper" , subjectMapper);
        ReflectionTestUtils.setField(subjectResource, "projectRepository" , projectRepository);
        this.restSubjectMockMvc = MockMvcBuilders.standaloneSetup(subjectResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        DeviceTypeResource deviceTypeResource = new DeviceTypeResource();
        ReflectionTestUtils.setField(deviceTypeResource, "deviceTypeService" , deviceTypeService);
        ReflectionTestUtils.setField(deviceTypeResource, "deviceTypeRepository" , deviceTypeRepository);
        this.restDeviceTypeMockMvc = MockMvcBuilders.standaloneSetup(deviceTypeResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Subject createEntity(EntityManager em) {
        Subject subject = new Subject()
            .externalLink(DEFAULT_EXTERNAL_LINK)
            .externalId(DEFAULT_ENTERNAL_ID)
            .removed(DEFAULT_REMOVED);
        return subject;
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SubjectDTO createEntityDTO(EntityManager em) {
        SubjectDTO subject = new SubjectDTO();
        subject.setExternalLink(DEFAULT_EXTERNAL_LINK);
        subject.setExternalId(DEFAULT_ENTERNAL_ID);
        subject.setEmail(DEFAULT_EMAIL);
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(1L);
        projectDTO.setProjectName("Radar");
        projectDTO.setLocation("SOMEWHERE");
        projectDTO.setDescription("test");
        subject.setProject(projectDTO);
        return subject;
    }

    @Before
    public void initTest() {
        subject = createEntity(em);
    }

    @Test
    @Transactional
    public void createSubject() throws Exception {
        int databaseSizeBeforeCreate = subjectRepository.findAll().size();

        // Create the Subject
        SubjectDTO subjectDTO = createEntityDTO(em);
        restSubjectMockMvc.perform(post("/api/subjects")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subjectDTO)))
            .andExpect(status().isCreated());

        // Validate the Subject in the database
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeCreate + 1);
        Subject testSubject = subjectList.get(subjectList.size() - 1);
        assertThat(testSubject.getExternalLink()).isEqualTo(DEFAULT_EXTERNAL_LINK);
        assertThat(testSubject.getExternalId()).isEqualTo(DEFAULT_ENTERNAL_ID);
        assertThat(testSubject.isRemoved()).isEqualTo(DEFAULT_REMOVED);
        assertEquals(testSubject.getUser().getRoles().size(),1);
    }

    @Test
    @Transactional
    public void createSubjectWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = subjectRepository.findAll().size();

        // Create the Subject with an existing ID
        subject.setId(1L);
        SubjectDTO subjectDTO = subjectMapper.subjectToSubjectDTO(subject);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubjectMockMvc.perform(post("/api/subjects")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subjectDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllSubjects() throws Exception {
        // Initialize the database
        subjectRepository.saveAndFlush(subject);

        // Get all the subjectList
        restSubjectMockMvc.perform(get("/api/subjects?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subject.getId().intValue())))
            .andExpect(jsonPath("$.[*].externalLink").value(hasItem(DEFAULT_EXTERNAL_LINK.toString())))
            .andExpect(jsonPath("$.[*].enternalId").value(hasItem(DEFAULT_ENTERNAL_ID.toString())))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())));
    }

    @Test
    @Transactional
    public void getSubject() throws Exception {
        // Initialize the database
        subjectRepository.saveAndFlush(subject);

        // Get the subject
        restSubjectMockMvc.perform(get("/api/subjects/{id}", subject.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(subject.getId().intValue()))
            .andExpect(jsonPath("$.externalLink").value(DEFAULT_EXTERNAL_LINK.toString()))
            .andExpect(jsonPath("$.enternalId").value(DEFAULT_ENTERNAL_ID.toString()))
            .andExpect(jsonPath("$.removed").value(DEFAULT_REMOVED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingSubject() throws Exception {
        // Get the subject
        restSubjectMockMvc.perform(get("/api/subjects/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSubject() throws Exception {
        // Initialize the database
        subjectRepository.saveAndFlush(subject);
        int databaseSizeBeforeUpdate = subjectRepository.findAll().size();

        // Update the subject
        Subject updatedSubject = subjectRepository.findOne(subject.getId());
        updatedSubject
            .externalLink(UPDATED_EXTERNAL_LINK)
            .externalId(UPDATED_ENTERNAL_ID)
            .removed(UPDATED_REMOVED);
        SubjectDTO subjectDTO = subjectMapper.subjectToSubjectDTO(updatedSubject);

        restSubjectMockMvc.perform(put("/api/subjects")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subjectDTO)))
            .andExpect(status().isOk());

        // Validate the Subject in the database
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeUpdate);
        Subject testSubject = subjectList.get(subjectList.size() - 1);
        assertThat(testSubject.getExternalLink()).isEqualTo(UPDATED_EXTERNAL_LINK);
        assertThat(testSubject.getExternalId()).isEqualTo(UPDATED_ENTERNAL_ID);
        assertThat(testSubject.isRemoved()).isEqualTo(UPDATED_REMOVED);
    }

    @Test
    @Transactional
    public void updateNonExistingSubject() throws Exception {
        int databaseSizeBeforeUpdate = subjectRepository.findAll().size();

        // Create the Subject
        SubjectDTO subjectDTO = subjectMapper.subjectToSubjectDTO(subject);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restSubjectMockMvc.perform(put("/api/subjects")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subjectDTO)))
            .andExpect(status().isCreated());

        // Validate the Subject in the database
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteSubject() throws Exception {
        // Initialize the database
        subjectRepository.saveAndFlush(subject);
        int databaseSizeBeforeDelete = subjectRepository.findAll().size();

        // Get the subject
        restSubjectMockMvc.perform(delete("/api/subjects/{id}", subject.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Subject.class);
    }


    @Test
    @Transactional
    public void dynamicSourceRegistration() throws Exception {
        int databaseSizeBeforeCreate = subjectRepository.findAll().size();

        String deviceModel = "App";
        String deviceProducer ="THINC-IT App";
        String deviceVersion = "v1";

        // Create the Subject
        SubjectDTO subjectDTO = createEntityDTO(em);
        restSubjectMockMvc.perform(post("/api/subjects")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subjectDTO)))
            .andExpect(status().isCreated());

        // Validate the Subject in the database
        List<Subject> subjectList = subjectRepository.findAll();
        assertThat(subjectList).hasSize(databaseSizeBeforeCreate + 1);
        Subject testSubject = subjectList.get(subjectList.size() - 1);

        String subjectLogin = testSubject.getUser().getLogin();
        assertNotNull(subjectLogin);

        AttributeMapDTO metadata = new AttributeMapDTO("some" , "value");

        SourceRegistrationDTO sourceRegistrationDTO = new SourceRegistrationDTO();
        sourceRegistrationDTO.setDeviceTypeModel(deviceModel);
        sourceRegistrationDTO.setDeviceTypeProducer(deviceProducer);
        sourceRegistrationDTO.setDeviceCatalogVersion(deviceVersion);
        sourceRegistrationDTO.getMetaData().add(metadata);
        assertThat(sourceRegistrationDTO.getSourceId()).isNull();
        // An entity with an existing ID cannot be created, so this API call must fail
        restSubjectMockMvc.perform(post("/api/subjects/{login}/sources", subjectLogin)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(sourceRegistrationDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceId").isNotEmpty());

        assertThat(sourceRegistrationDTO.getSourceId()).isNull();
        restSubjectMockMvc.perform(post("/api/subjects/{login}/sources", subjectLogin)
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(sourceRegistrationDTO)))
            .andExpect(status().is4xxClientError());
    }
}
