import { expect, test } from '@playwright/test';

test.describe('public access', () => {
  test('displays the French landing page and switches to registration', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Gestion de projet pour équipes logicielles exigeantes' }))
      .toBeVisible();
    await expect(page.getByRole('button', { name: 'Connexion' })).toBeVisible();
    await expect(page.getByLabel('Email')).toHaveValue('alice.admin@pmt.local');

    await page.getByRole('button', { name: 'Inscription' }).click();

    await expect(page.getByText('Inscription').last()).toBeVisible();
    await expect(page.getByLabel("Nom d'utilisateur")).toBeVisible();
    await expect(page.getByRole('button', { name: 'Créer le compte' })).toBeVisible();
  });
});
