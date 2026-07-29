package com.dragonaltar.eligibility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EligibilityRulesTest {
    @Test void allChecksMustPass(){EligibilitySnapshot valid=new EligibilitySnapshot(true,true,true,true,true,true,true,true,true,true);assertTrue(EligibilityRules.evaluate(valid).eligible());EligibilitySnapshot afk=new EligibilitySnapshot(true,true,true,true,true,true,false,true,true,true);var result=EligibilityRules.evaluate(afk);assertFalse(result.eligible());assertFalse(result.checks().get("not-afk"));}
    @Test void exposesEveryDecisionForExplainCommand(){var result=EligibilityRules.evaluate(new EligibilitySnapshot(false,false,false,false,false,false,false,false,false,false));assertEquals(10,result.checks().size());assertTrue(result.checks().values().stream().noneMatch(Boolean::booleanValue));}
}
