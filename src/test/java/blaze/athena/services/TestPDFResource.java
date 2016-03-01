package blaze.athena.services;

import blaze.athena.application.AthenaRestApplication;
import blaze.athena.application.AthenaSpringApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * <p>DOESNT WORK YET</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 01 Mar 2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=AthenaSpringApplication.class)
@WebAppConfiguration
public class TestPDFResource {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void resourceUploadSuccess() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File pdfFile = new File(classLoader.getResource("2014_Part_1_Lecture_01_Introduction.pdf").getFile());

        MockMultipartFile multipartInputFile = new MockMultipartFile("uploadedFile", pdfFile.getName(), "multipart/form-data", new FileInputStream(pdfFile));
        mockMvc.perform(MockMvcRequestBuilders.fileUpload(new URI(AthenaRestApplication.BASEURI + "/pdf/upload"))
                    .file(multipartInputFile))
                .andExpect(status().is(200));
    }

}
