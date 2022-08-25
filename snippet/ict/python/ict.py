import base64

class IctMetadataBuilder(object):
    @staticmethod
    def get_translation_with_metadata(repository_name, asset_name, text_unit_name, locale, stack, translation, is_translated):
        text_unit_metadata = IctMetadataBuilder._get_text_unit_metadata(
            repository_name, asset_name, text_unit_name, locale, stack, is_translated
        )
        text_unit_metadata_as_tags_block = TagsBlockEncoder.unicode_to_tags_block(text_unit_metadata)
        return '{startDelimiter}{text_unit_metadata_as_tags_block}{middleDelimiter}{translation}{endDelimiter}'.format(
            startDelimiter=IctMetadataBuilder._get_start_delimiter(),
            text_unit_metadata_as_tags_block=text_unit_metadata_as_tags_block,
            middleDelimiter=IctMetadataBuilder._get_middle_delimiter(),
            translation=translation,
            endDelimiter=IctMetadataBuilder._get_end_delimiter(),
        )

    @staticmethod
    def _get_text_unit_metadata(repository_name, asset_name, text_unit_name, locale, stack, is_translated):
        return '{repository_name}{delim}{asset_name}{delim}{text_unit_name}{delim}{locale}{delim}{stack}{delim}{is_translated}'.format(
            repository_name=repository_name,
            asset_name=asset_name if asset_name else "",
            text_unit_name=text_unit_name,
            locale=locale,
            stack=stack,
            delim=IctMetadataBuilder._get_inner_delimiter(),
            is_translated=is_translated
        )

    @staticmethod
    def _get_start_delimiter():
        return '\U000E0022'

    @staticmethod
    def _get_middle_delimiter():
        return '\U000E0023'

    @staticmethod
    def _get_end_delimiter():
        return '\U000E0024'

    @staticmethod
    def _get_inner_delimiter():
        return '\U00000013'


class TagsBlockEncoder(object):
    @staticmethod
    def unicode_to_tags_block(string):
        b64_string = base64.b64encode(string.encode('utf-8'))
        return TagsBlockEncoder._ascii_to_tags_block(b64_string)

    @staticmethod
    def _ascii_to_tags_block(string):
        res = ''
        for c in string:
            if c >= 32 and c <= 126:
                res = res + chr(917536 - 32 + c)
            else:
                raise 'Unsupported character to encode in Tags block.'
        return res



class GettextUtils(object):
    @staticmethod
    def get_text_unit_name(message, context, po_plural_form, locale, escape_context=False):
        """Follows same naming convention as Mojito to compute the text unit name for gettext/po files"""
        text_unit_name = message

        if context:
            # escaping quotes from the context is only needed if the text unit
            # name must match the Okapi PO filter context parsing logic
            if escape_context:
                context = context.replace('"', '\\"')

            text_unit_name = f"{text_unit_name} --- {context}"

        if po_plural_form is not None:
            text_unit_name = "{} _{}".format(
                text_unit_name, GettextUtils.po_plural_form_to_cldr_form(po_plural_form, locale)
            )

        return text_unit_name

    @staticmethod
    def po_plural_form_to_cldr_form(po_plural_form, locale):
        # Incomplete map, could be fetch from ICU
        po_to_cldr_map_by_locale = {
            'cs-CZ': {0: 'one', 1: 'few', 2: 'other'},
            'ja-JP': {0: 'other'},
            'ko-KR': {0: 'other'},
            'pl-PL': {0: 'one', 1: 'few', 2: 'many'},
            'ro-RO': {0: 'one', 1: 'few', 2: 'other'},
            'ru-RU': {0: 'one', 1: 'few', 2: 'many'},
            'sk-SK': {0: 'one', 1: 'few', 2: 'other'},
            'uk-UA': {0: 'one', 1: 'few', 2: 'many'},
            'vi-VN': {0: 'other'},
            'id-ID': {0: 'other'},
            'th-TH': {0: 'other'},
            'ms-MY': {0: 'other'},
            'zh-CN': {0: 'other'},
            'zh-TW': {0: 'other'},
            'he-IL': {0: 'one', 1: 'two', 2: 'many', 3: 'other'},
            'ar-SA': {0: 'zero', 1: 'one', 2: 'two', 3: 'few', 4: 'many', 5: 'other'},
        }

        po_to_cldr_map = po_to_cldr_map_by_locale.get(locale, {0: 'one', 1: 'other'})
        return po_to_cldr_map.get(po_plural_form, 'other')
