package com.hw.integration.bbs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CommentTest {
    @Autowired
    UserAction action;
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    UUID uuid;
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            action.saveResult(description,uuid);
            log.error("test failed, method {}, uuid {}", description.getMethodName(), uuid);
        }
    };
    @Test
    public void create_comment() {

    }

    @Test
    public void create_comment_w_invalid_reference() {

    }

    @Test
    public void delete_comment() {

    }

    @Test
    public void delete_comment_w_wrong_id() {

    }

    @Test
    public void get_comments_for_reference() {

    }

    @Test
    public void get_comments_for_user() {

    }
}
