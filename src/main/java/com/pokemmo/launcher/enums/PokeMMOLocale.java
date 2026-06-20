package com.pokemmo.launcher.enums;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.pokemmo.launcher.util.Util;

/**
 * @author Kyu
 */
public enum PokeMMOLocale
{
	EN("English", "en", true),
	FR("Français", "fr", true),
	ES("Español", "es", true),
	DE("Deutsche", "de", true),
	IT("Italiano", "it", true),
	PT_BR("Português (Brasileiro)", "pt-BR", true),
	KO("한국어", "ko", true),
	JA("日本語", "ja", true),
	ZH("中国人", "zh", true),
	ZH_HANT("中國人", "zh-Hant", true),
	FIL("Filipino", "fil", true),
	RU("Русские", "ru", false),
	PL("Polski", "pl", false);

	private final String display_name, lang_tag;
	private final boolean language_is_selectable;

	public static final PokeMMOLocale[] ENABLED_LANGUAGES;
	public static final Map<PokeMMOLocale, ResourceBundle> RESOURCES = new HashMap<>();

	static
	{
		ENABLED_LANGUAGES = Stream.of(values()).filter(PokeMMOLocale::isEnabled).toArray(PokeMMOLocale[]::new);
		for(var v : ENABLED_LANGUAGES)
		{
			RESOURCES.put(v, ResourceBundle.getBundle("MessagesBundle", Locale.forLanguageTag(v.getLangTag())));
		}
	}

	PokeMMOLocale(String display_name, String lang_tag, boolean language_is_selectable)
	{
		this.display_name = display_name;
		this.lang_tag = lang_tag;
		this.language_is_selectable = language_is_selectable;
	}

	public String getDisplayName()
	{
		return display_name;
	}

	public String getLangTag()
	{
		return lang_tag;
	}

	public String getLangTagShort()
	{
		if(lang_tag.indexOf('-') < 0)
			return lang_tag;
		return lang_tag.substring(0, lang_tag.indexOf('-'));
	}

	public boolean isEnabled()
	{
		return language_is_selectable;
	}

	public ResourceBundle getStrings()
	{
		return RESOURCES.getOrDefault(this, RESOURCES.get(PokeMMOLocale.EN));
	}

	public static PokeMMOLocale getFromLocale(Locale locale)
	{
		String lang = locale.getLanguage();

		if(!locale.getCountry().isEmpty())
		{
			lang += "-" + locale.getCountry();
		}

		//This happens for things like zh_CN_#Hant
		if(locale.getScript().equalsIgnoreCase("hant"))
		{
			lang = "zh-Hant";
		}

		if(Util.startsWithIgnoreCase(lang, "zh-TW") || Util.startsWithIgnoreCase(lang, "zh-HK"))
		{
			lang = "zh-Hant";
		}

		//Multiple, remove last. This happens for things like zh_CN_#Hans
		while(lang.indexOf('-') != lang.lastIndexOf('-'))
		{
			lang = lang.substring(0, lang.lastIndexOf('-'));
		}

		return getFromString(lang);
	}

	public static PokeMMOLocale getFromString(String lang)
	{
		PokeMMOLocale partial = null;
		for(PokeMMOLocale pokeMMOLocale : values())
		{
			//Prefer a perfect match
			if(pokeMMOLocale.getLangTag().equalsIgnoreCase(lang))
			{
				return pokeMMOLocale;
			}

			if(Util.startsWithIgnoreCase(lang, pokeMMOLocale.getLangTagShort()))
			{
				//Pick the less specific, eg, pt-PT should pick pt over pt-BR
				if(partial == null || pokeMMOLocale.getLangTag().length() < partial.getLangTag().length())
				{
					partial = pokeMMOLocale;
				}
			}
		}

		if(partial != null)
			return partial;

		// Default
		return PokeMMOLocale.EN;
	}

	public static PokeMMOLocale getDefaultLocale()
	{
		return getFromLocale(Locale.getDefault());
	}

	@Override
	public String toString()
	{
		return getDisplayName();
	}
}
