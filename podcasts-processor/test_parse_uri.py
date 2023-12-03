from unittest import TestCase

import utils


class TestParseUri(TestCase):

    PODCAST_RMQ_ADDRESS = "amqp://USER:PW@HOST/VHOST"

    def test_parse_uri(self):
        results = utils.parse_uri(self.PODCAST_RMQ_ADDRESS)
        self.assertEqual(results["scheme"], "amqp")
        self.assertEqual(results["path"], "VHOST")
        self.assertEqual(results["username"], "USER")
        self.assertEqual(results["password"], "PW")
        self.assertEqual(results["host"], "HOST")


if __name__ == "__main__":
    import unittest

    unittest.main()
