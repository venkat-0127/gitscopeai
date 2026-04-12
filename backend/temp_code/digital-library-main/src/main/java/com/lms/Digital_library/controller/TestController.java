package com.lms.Digital_library.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.Digital_library.entity.TestEntity;
import com.lms.Digital_library.entity.TestResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectMapper mapper;

    // List tests (id, title, questions)
    @GetMapping
    public List<Map<String,Object>> listTests(){
        List<TestEntity> list = em.createQuery("select t from TestEntity t order by t.createdAt desc", TestEntity.class)
                                  .getResultList();
        List<Map<String,Object>> out = new ArrayList<>();
        for(TestEntity t : list){
            Map<String,Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            try {
                m.put("questions", mapper.readValue(t.getQuestionsJson(), new TypeReference<List<Map<String,Object>>>(){}));
            } catch(Exception e){
                m.put("questions", Collections.emptyList());
            }
            out.add(m);
        }
        return out;
    }

    // Add test (admin) - simple
    @PostMapping
    @Transactional
    public ResponseEntity<?> addTest(@RequestBody Map<String,Object> body){
        String title = (String) body.get("title");
        Object q = body.get("questions");
        try {
            String qJson = mapper.writeValueAsString(q);
            TestEntity t = new TestEntity();
            t.setTitle(title);
            t.setQuestionsJson(qJson);
            em.persist(t);
            return ResponseEntity.ok(Map.of("id", t.getId()));
        } catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid questions JSON"));
        }
    }

    // Submit answers: grade and save result
    @PostMapping("/{id}/submit")
    @Transactional
    public ResponseEntity<?> submit(@PathVariable("id") Long id, @RequestBody Map<String,Object> payload){
        TestEntity t = em.find(TestEntity.class, id);
        if(t == null) return ResponseEntity.status(404).body(Map.of("error","test not found"));
        try {
            // parse stored questions
            List<Map<String,Object>> questions = mapper.readValue(t.getQuestionsJson(),
                    new TypeReference<List<Map<String,Object>>>(){});

            // Convert answers safely using ObjectMapper -> List<Integer>
            List<Integer> answers = Collections.emptyList();
            Object ansObj = payload.get("answers");
            if(ansObj != null){
                answers = mapper.convertValue(ansObj, new TypeReference<List<Integer>>() {});
            }

            int total = questions.size();
            int correct = 0;
            List<Map<String,Object>> feedback = new ArrayList<>();
            for(int i=0;i<questions.size();i++){
                Map<String,Object> q = questions.get(i);
                Integer correctIndex = null;
                Object ai = q.get("answerIndex");
                if(ai instanceof Number) correctIndex = ((Number) ai).intValue();
                Integer given = (answers != null && i < answers.size()) ? answers.get(i) : null;
                boolean ok = given != null && correctIndex != null && given.equals(correctIndex);
                if(ok) correct++;
                Map<String,Object> single = new HashMap<>();
                single.put("q", q.get("q"));
                single.put("selected", given);
                single.put("correct", correctIndex);
                feedback.add(single);
            }
            int scorePct = Math.round( (correct * 100f) / Math.max(1, total) );
            TestResult tr = new TestResult();
            tr.setTestId(id);
            tr.setUserEmail( (String) payload.getOrDefault("email", "guest") );
            tr.setScore(scorePct);
            tr.setTotal(total);
            tr.setDetailsJson(mapper.writeValueAsString(feedback));
            em.persist(tr);
            Map<String,Object> resp = Map.of("score", scorePct, "correct", correct, "total", total, "feedback", feedback);
            return ResponseEntity.ok(resp);
        } catch(Exception ex){
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error","grading failed"));
        }
    }

    // List results for a test (optional filter by email)
    @GetMapping("/{id}/results")
    public List<Map<String,Object>> results(@PathVariable("id") Long id, @RequestParam(required=false) String email){
        String ql = "select r from TestResult r where r.testId = :tid " + (email != null ? "and r.userEmail = :email " : "") + "order by r.createdAt desc";
        TypedQuery<TestResult> q = em.createQuery(ql, TestResult.class);
        q.setParameter("tid", id);
        if(email != null) q.setParameter("email", email);
        List<TestResult> list = q.getResultList();
        List<Map<String,Object>> out = new ArrayList<>();
        for(TestResult r : list){
            Map<String,Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("score", r.getScore());
            m.put("total", r.getTotal());
            m.put("userEmail", r.getUserEmail());
            m.put("details", r.getDetailsJson());
            m.put("createdAt", r.getCreatedAt());
            out.add(m);
        }
        return out;
    }
}
