from unittest import mock

import unittest

from ict import GettextUtils
from ict import IctMetadataBuilder
from ict import TagsBlockEncoder

# Keep this snippet in sync with the Javascript implementation IctMetadataExtractor.js, TagsBlockDecoder.js, etc
# in this directory
# python3 -m venv ~/p3-env
# . ~/p3-env/bin/activate
# python test_ict.py

class GettextUtilsTestCase(unittest.TestCase):
    def test_po_plural_form_to_cldr_form_en_US(self):
        self.assertEqual('one', GettextUtils.po_plural_form_to_cldr_form(0, 'en-US'))
        self.assertEqual('other', GettextUtils.po_plural_form_to_cldr_form(1, 'en-US'))

    def test_po_plural_form_to_cldr_form_fr_FR(self):
        self.assertEqual('one', GettextUtils.po_plural_form_to_cldr_form(0, 'fr-FR'))
        self.assertEqual('other', GettextUtils.po_plural_form_to_cldr_form(1, 'fr-FR'))

    def test_po_plural_form_to_cldr_form_ja_JP(self):
        self.assertEqual('other', GettextUtils.po_plural_form_to_cldr_form(0, 'ja-JP'))

    def test_po_plural_form_to_cldr_form_ru_RU(self):
        self.assertEqual('one', GettextUtils.po_plural_form_to_cldr_form(0, 'ru-RU'))
        self.assertEqual('few', GettextUtils.po_plural_form_to_cldr_form(1, 'ru-RU'))
        self.assertEqual('many', GettextUtils.po_plural_form_to_cldr_form(2, 'ru-RU'))

    def test_po_plural_form_to_cldr_form_unsupported(self):
        self.assertEqual('one', GettextUtils.po_plural_form_to_cldr_form(0, 'unsupported'))
        self.assertEqual('other', GettextUtils.po_plural_form_to_cldr_form(1, 'unsupported'))
        self.assertEqual('other', GettextUtils.po_plural_form_to_cldr_form(2, 'unsupported'))

    def test_get_text_unit_name_no_context(self):
        self.assertEqual('msg', GettextUtils.get_text_unit_name('msg', None, None, 'en'))

    def test_get_text_unit_name_with_context(self):
        self.assertEqual('msg --- ctx', GettextUtils.get_text_unit_name('msg', 'ctx', None, 'en'))

    def test_get_text_unit_name_with_plural_en_singular(self):
        self.assertEqual('msg --- ctx _one', GettextUtils.get_text_unit_name('msg', 'ctx', 0, 'en'))

    def test_get_text_unit_name_with_plural_en_plural(self):
        self.assertEqual('msg --- ctx _other', GettextUtils.get_text_unit_name('msg', 'ctx', 1, 'en'))

    def test_get_text_unit_name_with_plural_ru_plural_one(self):
        self.assertEqual('msg --- ctx _one', GettextUtils.get_text_unit_name('msg', 'ctx', 0, 'ru-RU'))

    def test_get_text_unit_name_with_plural_ru_plural_few(self):
        self.assertEqual('msg --- ctx _few', GettextUtils.get_text_unit_name('msg', 'ctx', 1, 'ru-RU'))

    def test_get_text_unit_name_with_plural_ru_plural_many(self):
        self.assertEqual('msg --- ctx _many', GettextUtils.get_text_unit_name('msg', 'ctx', 2, 'ru-RU'))


class IctMetadataBuilderTestCase(unittest.TestCase):
    def test_unicode_to_tags_block(self):
        expected = (
            '\U000e0022\U000e0063\U000e006d\U000e0056\U000e0077\U000e0062\U000e0078\U000e004e'
            '\U000e0068\U000e0063\U000e0033\U000e004e\U000e006c\U000e0064\U000e0042\U000e004e'
            '\U000e0030\U000e005a\U000e0058\U000e0068\U000e0030\U000e0064\U000e0057\U000e0035'
            '\U000e0070\U000e0064\U000e0042\U000e004e\U000e006d\U000e0063\U000e0069\U000e0031'
            '\U000e0047\U000e0055\U000e0068\U000e004e\U000e007a\U000e0064\U000e0047\U000e0046'
            '\U000e006a\U000e0061\U000e0077\U000e003d\U000e003d\U000e0023translation3\U000e0024'
        )
        actual = IctMetadataBuilder.get_translation_with_metadata(
            'repo', 'asset', 'textunit', 'fr-FR', 'stack', 'translation3'
        )
        # _print_actual_for_test_update(actual)
        self.assertEqual(expected, actual)


class TagsBlockEncoderTestCase(unittest.TestCase):
    def test_unicode_to_tags_block_basic_ascii(self):
        expected = '\U000e0059\U000e0057\U000e0046\U000e0069\U000e0059\U000e006d\U000e004e\U000e006a'
        actual = TagsBlockEncoder.unicode_to_tags_block('aabbcc')
        # _print_actual_for_test_update(actual)
        self.assertEqual(expected, actual)

    def test_unicode_to_tags_block_japanese(self):
        expected = (
            '\U000e0035\U000e0070\U000e0065\U000e006c\U000e0035\U000e0070\U000e0079\U000e0073'
            '\U000e0035\U000e005a\U000e0075\U000e0039'
        )
        actual = TagsBlockEncoder.unicode_to_tags_block('日本国')
        # _print_actual_for_test_update(actual)
        self.assertEqual(expected, actual)


def _print_actual_for_test_update(actual):
    print(actual.encode('raw_unicode_escape').decode('utf-8'))

if __name__ == '__main__':
    unittest.main()