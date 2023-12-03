import random
from unittest import TestCase

import retry

ctr = 0


@retry.retryable
def problem_with_decorator(name: str) -> str:
    return "hello %s" % name


def add() -> int:
    global ctr
    ctr += 1
    random_int: int = random.randint(1, 10)
    print("random_int: %s" % random_int)
    if random_int >= 5:
        raise Exception("something broke!")
    return random_int


def problem():
    raise Exception("noway jose the cat from the dollop")


class TestRetry(TestCase):
    def test_retryable_decorator(self):
        jane = "Jane"
        result = problem_with_decorator(jane)
        assert result == "hello %s" % jane

    #
    # def test_retry_with_fallback(self):
    #     result = retry.retry(problem, max_retries=0, fallback=lambda: 10)
    #     self.assertEqual(result, 10)
    #
    # def test_retry_no_retries_no_fallback(self):
    #     result = retry.retry(problem, max_retries=0)
    #     self.assertTrue(result is None)
    #
    # def test_retry_happy_path(self):
    #     result = retry.retry(add, fallback=lambda: 10, max_retries=5)
    #     self.assertTrue(ctr <= 5)
    #     print(result)


if __name__ == "__main__":
    import unittest

    unittest.main()
