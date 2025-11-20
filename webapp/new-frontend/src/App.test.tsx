import { render, screen } from '@testing-library/react';

import { App } from './App';

describe('App', () => {
  it('renders the headline', () => {
    render(<App />);
    expect(screen.getByText(/New Frontend/i)).toBeInTheDocument();
  });
});
