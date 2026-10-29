"""Prove the guard rejects implementation imports as well as qualified bypasses."""
import sys
from pathlib import Path
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_architecture import violations


class ArchitectureGuardTest(unittest.TestCase):
    def test_presentation_rejects_data_import_alias(self):
        self.assertTrue(violations('/features/auth/presentation/Auth.kt', '/** Auth state. */\nimport com.pawan.kmpnativeuitrading.features.trading.data.remote.ApiFailure as Failure'))

    def test_domain_rejects_fully_qualified_constructor(self):
        self.assertTrue(violations('/features/trading/domain/Rule.kt', '/** Trading rule. */\nval client = com.pawan.kmpnativeuitrading.features.trading.data.remote.RemoteAccount()'))

    def test_domain_contract_and_composition_root_are_allowed(self):
        self.assertFalse(violations('/features/trading/presentation/Model.kt', '/** Screen state. */\nimport com.pawan.kmpnativeuitrading.features.trading.domain.repository.TradingSession'))
        self.assertFalse(violations('/core/session/AppSession.kt', '/** Wiring. */\nimport com.pawan.kmpnativeuitrading.features.trading.data.remote.RemoteAccount'))

    def test_missing_documentation_fails(self):
        self.assertTrue(violations('/features/auth/domain/Auth.kt', 'package example'))


if __name__ == '__main__':
    unittest.main()
