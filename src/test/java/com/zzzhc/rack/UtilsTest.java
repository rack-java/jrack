package com.zzzhc.rack;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UtilsTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testParseNestedQuery() {
		Map<String, Object> params = Utils.parseNestedQuery("a=1&b=2",
				Utils.DEFAULT_SEP);
		assertEquals("1", params.get("a"));
		assertEquals("2", params.get("b"));
		assertEquals(2, params.size());

		params = Utils.parseNestedQuery(
				"a[]=1&user[name]=u&user[password]=xyz", Utils.DEFAULT_SEP);
		assertTrue(params.get("a") instanceof List);
		List<Object> a = (List<Object>) params.get("a");
		assertEquals(1, a.size());
		assertEquals("1", a.get(0));
		assertTrue(params.get("user") instanceof Map);
		Map<String, Object> user = (Map<String, Object>) params.get("user");
		assertEquals("u", user.get("name"));
		assertEquals("xyz", user.get("password"));

		String qs = "users[][name]=u1&users[][password]=xyz1&users[][name]=u2&users[][password]=xyz2";
		params = Utils.parseNestedQuery(qs, Utils.DEFAULT_SEP);
		assertTrue(params.get("users") instanceof List);
		List<Map<String, Object>> users = (List<Map<String, Object>>) params
				.get("users");
		user = users.get(0);
		assertEquals("u1", user.get("name"));
		assertEquals("xyz1", user.get("password"));
		user = users.get(1);
		assertEquals("u2", user.get("name"));
		assertEquals("xyz2", user.get("password"));
	}

}
